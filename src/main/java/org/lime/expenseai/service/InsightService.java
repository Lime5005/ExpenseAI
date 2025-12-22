package org.lime.expenseai.service;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.lime.expenseai.model.Insight;
import org.lime.expenseai.model.MonthlySummary;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.Locale;

@Service
public class InsightService {

    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("org.lime.expenseai");

    private final ExpenseService expenseService;
    private final ChatClient chatClient;

    public InsightService(ExpenseService expenseService, ChatClient.Builder chatClientBuilder) {
        this.expenseService = expenseService;
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a financial insights assistant. Given a monthly expense summary, produce concise insights:
                        - Identify top spending categories.
                        - Compare to last month (percentage already provided).
                        - Highlight recurring/periodic patterns if evident.
                        - Give actionable saving suggestions.
                        Keep it factual, 3-5 sentences max.
                        Do not assume any currency; if a currency is not provided, present amounts without symbols.""")
                .build();
    }

    public Insight analyze(YearMonth month, String language, String currency) {
        Span span = tracer.spanBuilder("insight.analyze").startSpan();
        try (Scope scope = span.makeCurrent()) {
            MonthlySummary summary = expenseService.buildMonthlySummary(month);
            String targetLanguage = normalizeLanguage(language);
            String currencyHint = normalizeCurrency(currency);
            String prompt = """
                    Analyze this monthly summary and return insights in %s.
                    Currency: %s. If provided, use this currency in amounts and do not use any other currency symbol.
                    Summary: %s
                    """.formatted(targetLanguage, currencyHint, summary);

            Span llmSpan = tracer.spanBuilder("llm.chat").setSpanKind(SpanKind.CLIENT).startSpan();
            String reply;
            try (Scope llmScope = llmSpan.makeCurrent()) {
                long startNanos = System.nanoTime();
                ChatResponse response = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .chatResponse();
                long latencyNanos = System.nanoTime() - startNanos;
                llmSpan.setAttribute("llm.latency", latencyNanos / 1_000_000_000d);

                if (response != null && response.getMetadata() != null) {
                    String model = response.getMetadata().getModel();
                    if (model != null && !model.isBlank()) {
                        llmSpan.setAttribute("llm.model", model);
                    }
                    Usage usage = response.getMetadata().getUsage();
                    if (usage != null) {
                        Integer inputTokens = usage.getPromptTokens();
                        Integer outputTokens = usage.getCompletionTokens();
                        if (inputTokens != null) {
                            llmSpan.setAttribute("llm.input_tokens", inputTokens);
                        }
                        if (outputTokens != null) {
                            llmSpan.setAttribute("llm.output_tokens", outputTokens);
                        }
                    }
                }
                reply = (response == null || response.getResult() == null)
                        ? null
                        : response.getResult().getOutput().getText();
            } catch (Exception e) {
                llmSpan.recordException(e);
                llmSpan.setStatus(StatusCode.ERROR);
                throw e;
            } finally {
                llmSpan.end();
            }

            String content = (reply == null) ? "" : reply.trim();
            return new Insight(content);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            span.end();
        }
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) return "english";
        String key = language.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "en", "english", "en-us", "en-uk" -> "English";
            case "fr", "french", "fr-fr", "fr-ca" -> "French";
            case "zh", "chinese", "zh-cn", "zh-tw" -> "Chinese";
            default -> "english";
        };
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "none";
        }
        return currency.trim();
    }
}

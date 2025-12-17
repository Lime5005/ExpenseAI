package org.lime.expenseai.service;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.lime.expenseai.tool.ExpenseTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("org.lime.expenseai");

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder, ExpenseTools tools) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are an expense assistant. Always call tools to read or write expense data.
                        - To add an expense, call addExpense with date, category, amount, description.
                        - To list expenses, call getExpensesByDate or getExpensesByMonth.
                        - To summarize a month, call getMonthlyTotals (yyyy-MM) and report only the returned totals and totalAmount; do not do your own math.
                        - To correct an expense: if user provides an id, call updateExpense; otherwise call updateExpenseByDateAndDescription with the date/description the user mentioned. Do not add a new record for corrections.
                        Known categories: FOOD, GROCERIES, ENTERTAINMENT, TRANSPORT, SHOPPING.
                        If the category is ambiguous or unknown, call classifyCategoryByEmbedding with the expense description to get the category.
                        Do not fabricate records or categories; always rely on tool outputs.""")
                .defaultTools(tools)
                .build();
    }

    public String chat(String userMessage) {
        Span llmSpan = tracer.spanBuilder("llm.chat").setSpanKind(SpanKind.CLIENT).startSpan();
        try (Scope scope = llmSpan.makeCurrent()) {
            long startNanos = System.nanoTime();
            ChatResponse response = chatClient.prompt()
                    .user(userMessage)
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

            if (response == null || response.getResult() == null) {
                return "";
            }
            String text = response.getResult().getOutput().getText();
            return text == null ? "" : text;
        } catch (Exception e) {
            llmSpan.recordException(e);
            llmSpan.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            llmSpan.end();
        }
    }
}

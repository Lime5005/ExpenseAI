package org.lime.expenseai.service;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.lime.expenseai.model.Category;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Very small in-memory classifier: embeds known categories and picks nearest.
 */
@Component
public class CategoryClassifier {

    private static final Logger log = Logger.getLogger(CategoryClassifier.class.getName());
    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("org.lime.expenseai");

    private final EmbeddingModel embeddingModel;
    private final String embeddingModelName;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private static final List<Category> CATEGORIES = List.of(Category.values());

    private final Map<Category, float[]> categoryVectors = new HashMap<>();

    public CategoryClassifier(
            EmbeddingModel embeddingModel,
            @Value("${spring.ai.ollama.embedding.model:nomic-embed-text:latest}") String embeddingModelName
    ) {
        this.embeddingModel = embeddingModel;
        this.embeddingModelName = embeddingModelName;
    }

    public String classify(String text) {
        log.info("CategoryClassifier.classify called: " + text);
        initIfNeeded();
        EmbeddingResponse response = embedWithSpan("llm.embedding", List.of(text));
        float[] query = response.getResult().getOutput();
        return categoryVectors.entrySet().stream()
                .max((a, b) -> Float.compare(cosineSimilarity(query, a.getValue()), cosineSimilarity(query, b.getValue())))
                .map(entry -> entry.getKey().name())
                .orElse("OTHER");
    }

    private void initIfNeeded() {
        if (initialized.compareAndSet(false, true)) {
            EmbeddingResponse resp = embedWithSpan("llm.embedding.categories_init", CATEGORIES.stream().map(Enum::name).toList());
            for (int i = 0; i < CATEGORIES.size(); i++) {
                categoryVectors.put(CATEGORIES.get(i), resp.getResults().get(i).getOutput());
            }
        }
    }

    private EmbeddingResponse embedWithSpan(String spanName, List<String> inputs) {
        Span span = tracer.spanBuilder(spanName).setSpanKind(SpanKind.CLIENT).startSpan();
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("llm.operation", "embedding");
            long startNanos = System.nanoTime();
            EmbeddingResponse response = embeddingModel.embedForResponse(inputs);
            long latencyNanos = System.nanoTime() - startNanos;
            span.setAttribute("llm.latency", latencyNanos / 1_000_000_000d);

            if (response != null && response.getMetadata() != null) {
                String model = response.getMetadata().getModel();
                if (model != null && !model.isBlank()) {
                    span.setAttribute("llm.model", model);
                } else if (embeddingModelName != null && !embeddingModelName.isBlank()) {
                    span.setAttribute("llm.model", embeddingModelName);
                }
                Usage usage = response.getMetadata().getUsage();
                if (usage != null) {
                    Integer inputTokens = usage.getPromptTokens();
                    Integer outputTokens = usage.getCompletionTokens();
                    if (inputTokens != null) {
                        span.setAttribute("llm.input_tokens", inputTokens);
                    }
                    if (outputTokens != null) {
                        span.setAttribute("llm.output_tokens", outputTokens);
                    }
                }
            }
            if ((response == null || response.getMetadata() == null)
                    && embeddingModelName != null
                    && !embeddingModelName.isBlank()) {
                span.setAttribute("llm.model", embeddingModelName);
            }
            return response;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            span.end();
        }
    }

    private float cosineSimilarity(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return -1;
        return (float) (dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }
}

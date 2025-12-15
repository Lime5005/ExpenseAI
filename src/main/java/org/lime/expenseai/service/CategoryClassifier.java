package org.lime.expenseai.service;

import org.lime.expenseai.model.Category;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Very small in-memory classifier: embeds known categories and picks nearest.
 */
@Component
public class CategoryClassifier {

    private final EmbeddingModel embeddingModel;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private static final List<Category> CATEGORIES = List.of(Category.values());

    private final Map<Category, float[]> categoryVectors = new HashMap<>();

    public CategoryClassifier(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String classify(String text) {
        initIfNeeded();
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        float[] query = response.getResult().getOutput();
        return categoryVectors.entrySet().stream()
                .max((a, b) -> Float.compare(cosineSimilarity(query, a.getValue()), cosineSimilarity(query, b.getValue())))
                .map(entry -> entry.getKey().name())
                .orElse(Category.OTHER.name());
    }

    private void initIfNeeded() {
        if (initialized.compareAndSet(false, true)) {
            EmbeddingResponse resp = embeddingModel.embedForResponse(CATEGORIES.stream().map(Enum::name).toList());
            for (int i = 0; i < CATEGORIES.size(); i++) {
                categoryVectors.put(CATEGORIES.get(i), resp.getResults().get(i).getOutput());
            }
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

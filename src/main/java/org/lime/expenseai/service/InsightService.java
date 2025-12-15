package org.lime.expenseai.service;

import org.lime.expenseai.model.Insight;
import org.lime.expenseai.model.MonthlySummary;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.Locale;

@Service
public class InsightService {

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
                        Keep it factual, 3-5 sentences max.""")
                .build();
    }

    public Insight analyze(YearMonth month, String language) {
        MonthlySummary summary = expenseService.buildMonthlySummary(month);
        String targetLanguage = normalizeLanguage(language);
        String prompt = """
                Analyze this monthly summary and return insights in %s.
                Summary: %s
                """.formatted(targetLanguage, summary);
        String reply = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        String content = (reply == null) ? "" : reply.trim();
        return new Insight(content);
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
}

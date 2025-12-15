package org.lime.expenseai.service;

import org.lime.expenseai.tool.ExpenseTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder, ExpenseTools tools) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are an expense assistant. Always call tools to read or write expense data.
                        - To add an expense, call addExpense with date, category, amount, description.
                        - To list expenses, call getExpensesByDate or getExpensesByMonth.
                        - To summarize a month, call getMonthlyTotals (yyyy-MM) and report only the returned totals and totalAmount; do not do your own math.
                        - To correct an expense: if user provides an id, call updateExpense; otherwise call updateExpenseByDateAndDescription with the date/description the user mentioned. Do not add a new record for corrections.
                        Allowed categories: FOOD, GROCERIES, ENTERTAINMENT, TRANSPORT, SHOPPING, OTHER.
                        Do not fabricate records or categories; always rely on tool outputs.""")
                .defaultTools(tools)
                .build();
    }

    public String chat(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }
}

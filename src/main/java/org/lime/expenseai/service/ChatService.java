package org.lime.expenseai.service;

import org.lime.expenseai.tool.ExpenseTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder, ExpenseTools tools) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are an expense assistant. Always call tools to read or write expense data.
                        - To add an expense, call addExpense with date, category, amount, description.
                        - To list expenses, call getExpensesByDate or getExpensesByMonth.
                        Do not fabricate records; use tools for all data operations.""")
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

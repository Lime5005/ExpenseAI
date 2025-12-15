package org.lime.expenseai.controller;

import org.lime.expenseai.model.ChatRequest;
import org.lime.expenseai.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public Map<String, String> chat(@RequestBody ChatRequest request) {
        String reply = chatService.chat(request.message());
        return Map.of("reply", reply);
    }
}

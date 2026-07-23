package com.br.mamba_wedding.messages.api;

import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.messages.api.dto.MessageRequest;
import com.br.mamba_wedding.messages.application.MessageService;
import com.br.mamba_wedding.messages.domain.Message;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<List<Message>> list() {
        return ResponseEntity.ok(messageService.listMessages());
    }

    @PostMapping
    public ResponseEntity<Message> create(
            @Valid @RequestBody MessageRequest request,
            @AuthenticationPrincipal Guest loggedGuest 
    ) {
        Message savedMessage = messageService.leaveMessage(loggedGuest.getFullName(), request.text());
        return ResponseEntity.ok(savedMessage);
    }
}

package com.br.mamba_wedding.messages.api;

import com.br.mamba_wedding.common.api.ApiPaths;
import com.br.mamba_wedding.common.api.PageResponse;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.messages.api.dto.MessageRequest;
import com.br.mamba_wedding.messages.application.MessageService;
import com.br.mamba_wedding.messages.domain.Message;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping(ApiPaths.V1 + "/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<Message>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String author
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sendDate", "id"));
        return ResponseEntity.ok(PageResponse.from(messageService.listMessages(author, pageable)));
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

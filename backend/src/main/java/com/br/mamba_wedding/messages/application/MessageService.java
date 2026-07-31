package com.br.mamba_wedding.messages.application;

import com.br.mamba_wedding.messages.domain.Message;
import com.br.mamba_wedding.messages.infrastructure.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Message leaveMessage(String author, String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("A mensagem não pode estar vazia.");
        }
        Message newMessage = new Message(author, text);
        return messageRepository.save(newMessage);
    }

    public Page<Message> listMessages(String author, Pageable pageable) {
        if (author == null || author.isBlank()) {
            return messageRepository.findAll(pageable);
        }
        return messageRepository.findByAuthorContainingIgnoreCase(author.trim(), pageable);
    }
}

package com.br.mamba_wedding.messages.infrastructure;

import com.br.mamba_wedding.messages.domain.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageRepository extends MongoRepository<Message, String> {
    Page<Message> findByAuthorContainingIgnoreCase(String author, Pageable pageable);
}

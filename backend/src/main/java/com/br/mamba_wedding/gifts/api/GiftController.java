package com.br.mamba_wedding.gifts.api;

import com.br.mamba_wedding.gifts.api.dto.GiftReserveRequest;
import com.br.mamba_wedding.gifts.api.dto.GiftResponse;
import com.br.mamba_wedding.gifts.application.GiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/gifts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GiftController {

    private final GiftService giftService;

    @GetMapping
    public ResponseEntity<List<GiftResponse>> list() {
        List<GiftResponse> response = giftService.listAll()
                .stream()
                .map(GiftResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<Void> reserve(@PathVariable Long id, @Valid @RequestBody GiftReserveRequest request) {
        giftService.reservar(id, request.reservadoPor());
        return ResponseEntity.noContent().build();
    }
}
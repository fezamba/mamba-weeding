package com.br.mamba_wedding.gifts.api;

import com.br.mamba_wedding.common.api.ApiPaths;
import com.br.mamba_wedding.common.api.PageResponse;
import com.br.mamba_wedding.gifts.api.dto.GiftDetail;
import com.br.mamba_wedding.gifts.api.dto.GiftList;
import com.br.mamba_wedding.gifts.api.dto.ReserveRequest;
import com.br.mamba_wedding.gifts.application.GiftService;
import com.br.mamba_wedding.gifts.domain.Gift;
import com.br.mamba_wedding.guests.domain.Guest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping(ApiPaths.V1 + "/gifts")
@RequiredArgsConstructor
public class GiftController {

    private final GiftService giftService;

    @GetMapping
    public ResponseEntity<PageResponse<GiftList>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String name
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        var response = giftService.listAll(name, pageable).map(GiftList::from);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GiftDetail> detailGift(@PathVariable Long id){
        Gift response = giftService.findById(id);
        return ResponseEntity.ok(GiftDetail.from(response));
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<Void> reserve(@PathVariable Long id, @AuthenticationPrincipal Guest loggedGuest, @Valid @RequestBody ReserveRequest request) {
        giftService.reserve(id, loggedGuest.getId(), request.quotas());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/reserve")
    public ResponseEntity<Void> cancelReserve(@PathVariable Long id, @AuthenticationPrincipal Guest loggedGuest){
        giftService.cancelReserve(id, loggedGuest.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/buy")
    public ResponseEntity<Void> buyGift(@PathVariable Long id, @AuthenticationPrincipal Guest loggedGuest){
        giftService.buy(id, loggedGuest.getId());
        return ResponseEntity.noContent().build();
    }
}

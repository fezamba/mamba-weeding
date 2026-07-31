package com.br.mamba_wedding.gifts.api;

import com.br.mamba_wedding.common.api.ApiPaths;
import com.br.mamba_wedding.common.api.PageResponse;
import com.br.mamba_wedding.gifts.api.dto.GiftCreate;
import com.br.mamba_wedding.gifts.api.dto.GiftCreated;
import com.br.mamba_wedding.gifts.api.dto.GiftList;
import com.br.mamba_wedding.gifts.application.GiftService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/events/{eventId}/gifts")
@RequiredArgsConstructor
public class AdminGiftController {

    private final GiftService giftService;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<PageResponse<GiftList>> list(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String name
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        var response = giftService.listAll(eventId, name, pageable).map(GiftList::from);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/register")
    public ResponseEntity<GiftCreated> registerGift(
            @PathVariable Long eventId,
            @Valid @RequestBody GiftCreate gift
    ) {
        GiftCreated response = giftService.register(eventId, gift);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> deleteGift(@PathVariable Long eventId, @PathVariable Long id){
        giftService.delete(eventId, id);
        return ResponseEntity.noContent().build();
    }
}

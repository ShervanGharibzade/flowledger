package org.fl.flowledger.transfer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fl.flowledger.transfer.dto.CreateTransferDto;
import org.fl.flowledger.transfer.dto.TransferResponse;
import org.fl.flowledger.transfer.service.TransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> create(
            @Valid @RequestBody CreateTransferDto dto,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return ResponseEntity.ok(
                transferService.createTransaction(dto, idempotencyKey)
        );
    }

    @GetMapping
    public ResponseEntity<List<TransferResponse>> myTransactions() {
        return ResponseEntity.ok(
                transferService.myTransactions()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> findById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                transferService.findTransaction(id)
        );
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID id
    ) {
        transferService.cancelTransfer(id);

        return ResponseEntity.noContent().build();
    }
}

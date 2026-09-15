package org.fl.flowledger.ledger.controller;

import lombok.RequiredArgsConstructor;
import org.fl.flowledger.ledger.dto.LedgerEntryResponse;
import org.fl.flowledger.ledger.service.LedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<List<LedgerEntryResponse>> findEntriesForWallet(
            @PathVariable UUID walletId
    ) {
        return ResponseEntity.ok(
                ledgerService.findEntriesForWallet(walletId)
        );
    }

    @GetMapping("/transfer/{transferId}")
    public ResponseEntity<List<LedgerEntryResponse>> findEntriesForTransfer(
            @PathVariable UUID transferId
    ) {
        return ResponseEntity.ok(
                ledgerService.findEntriesForTransfer(transferId)
        );
    }
}

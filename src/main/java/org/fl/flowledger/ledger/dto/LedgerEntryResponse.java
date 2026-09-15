package org.fl.flowledger.ledger.dto;

import org.fl.flowledger.wallet.dto.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID walletId,
        UUID transferId,
        LedgerType type,
        BigDecimal amount,
        Currency currency,
        Instant createdAt
) {
}

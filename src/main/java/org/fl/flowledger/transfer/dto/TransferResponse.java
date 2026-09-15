package org.fl.flowledger.transfer.dto;

import org.fl.flowledger.wallet.dto.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        String reference,
        BigDecimal amount,
        UUID receiverWallet,
        Currency currency,
        TransferStatus status,
        Instant createdAt,
        Instant completedAt
) {
}

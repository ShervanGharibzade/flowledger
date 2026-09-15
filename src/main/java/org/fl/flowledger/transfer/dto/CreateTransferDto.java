package org.fl.flowledger.transfer.dto;

import org.fl.flowledger.wallet.dto.Currency;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferDto(
        UUID senderWalletId,
        UUID receiverWalletId,
        BigDecimal amount,
        Currency currency
) {
}

package org.fl.flowledger.transfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.fl.flowledger.wallet.dto.Currency;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferDto(
        @NotNull UUID senderWalletId,
        @NotNull UUID receiverWalletId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        Currency currency
) {
}

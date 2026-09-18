package org.fl.flowledger.wallet.dto;

import jakarta.validation.constraints.NotNull;


public record CreateWalletDto(
        @NotNull
        Currency currency
) {
}

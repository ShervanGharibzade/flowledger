package org.fl.flowledger.wallet.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;


public record DeleteWalletDto(
        @NotNull
        UUID walletUuId
) {
}

package org.fl.flowledger.wallet.dto;

import java.util.UUID;

public record CreateWalletDto(
        UUID uuid,
        Currency currency
) {
}

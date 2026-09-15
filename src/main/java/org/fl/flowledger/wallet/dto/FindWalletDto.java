package org.fl.flowledger.wallet.dto;

import java.util.UUID;

public record FindWalletDto(
        UUID UserUuid,
        UUID walletUuId
) {
}

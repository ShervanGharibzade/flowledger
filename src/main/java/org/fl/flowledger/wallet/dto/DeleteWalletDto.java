package org.fl.flowledger.wallet.dto;

import java.util.UUID;

public record DeleteWalletDto(
        UUID UserUuid,
        UUID walletUuId
) {
}

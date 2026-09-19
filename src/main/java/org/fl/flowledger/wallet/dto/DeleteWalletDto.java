package org.fl.flowledger.wallet.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

// No user identifier here on purpose: ownership is always checked against
// the authenticated caller (see WalletController/WalletServiceImpl), never
// against a user id supplied by the client.
public record DeleteWalletDto(
        @NotNull
        UUID walletUuId
) {
}

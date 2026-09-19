package org.fl.flowledger.wallet.dto;

import jakarta.validation.constraints.NotNull;

// No user identifier here on purpose: a wallet is always created for the
// authenticated caller (see WalletController/WalletServiceImpl), never for
// an arbitrary user id supplied by the client.
public record CreateWalletDto(
        @NotNull
        Currency currency
) {
}

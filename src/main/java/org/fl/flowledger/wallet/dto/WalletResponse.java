package org.fl.flowledger.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        Currency currency,
        BigDecimal balance,
        WalletStatus status
) {
}

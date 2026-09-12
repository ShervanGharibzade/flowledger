package org.fl.flowledger.transfer.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.fl.flowledger.common.entity.BaseEntity;
import org.fl.flowledger.transfer.dto.TransferStatus;
import org.fl.flowledger.wallet.dto.Currency;
import org.fl.flowledger.wallet.entity.Wallet;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"senderWallet", "receiverWallet"})
public class Transfer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_wallet_id", nullable = false)
    private Wallet senderWallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_wallet_id", nullable = false)
    private Wallet receiverWallet;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            updatable = false,
            length = 100
    )
    private String idempotencyKey;

    @Column(
            name = "reference",
            unique = true,
            length = 100
    )
    private String reference;

    @Column(name = "completed_at")
    private Instant completedAt;
}
package org.fl.flowledger.ledger.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.fl.flowledger.common.entity.BaseEntity;
import org.fl.flowledger.ledger.dto.LedgerType;
import org.fl.flowledger.transfer.dto.TransferStatus;
import org.fl.flowledger.transfer.entity.Transfer;
import org.fl.flowledger.wallet.dto.Currency;
import org.fl.flowledger.wallet.entity.Wallet;

import java.math.BigDecimal;

@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"wallet"})
public class Ledger extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private Transfer transfer;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LedgerType type;
}
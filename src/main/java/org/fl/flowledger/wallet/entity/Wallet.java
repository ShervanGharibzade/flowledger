package org.fl.flowledger.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.fl.flowledger.common.entity.BaseEntity;
import org.fl.flowledger.user.entity.User;
import org.fl.flowledger.wallet.dto.Currency;
import org.fl.flowledger.wallet.dto.WalletStatus;

import java.math.BigDecimal;


@Entity
@Table(name = "Wallets")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"user"})
public class Wallet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency currency;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private WalletStatus status;

    @Version
    private Long version;

}

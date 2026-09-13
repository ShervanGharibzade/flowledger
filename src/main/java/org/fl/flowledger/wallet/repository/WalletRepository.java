package org.fl.flowledger.wallet.repository;

import org.fl.flowledger.wallet.dto.Currency;
import org.fl.flowledger.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<Wallet> findAllByUserId(Long userId);

    Optional<Wallet> findByUserIdAndCurrency(Long userId, Currency currency);

}

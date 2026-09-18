package org.fl.flowledger.wallet.repository;

import jakarta.persistence.LockModeType;
import org.fl.flowledger.wallet.dto.Currency;
import org.fl.flowledger.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<Wallet> findAllByUserId(Long id);

    boolean existsByUserIdAndCurrency(Long userId, Currency currency);

    Optional<Wallet> findByUserIdAndCurrency(Long userId, Currency currency);

    Optional<Wallet> findWalletByIdAndUserId(UUID WalletId, Long userId);

    Optional<Wallet> findByUuid(UUID uuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.uuid = :uuid")
    Optional<Wallet> findByUuidForUpdate(UUID uuid);
}

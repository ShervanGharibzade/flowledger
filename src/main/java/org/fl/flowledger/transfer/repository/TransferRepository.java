package org.fl.flowledger.transfer.repository;

import org.fl.flowledger.transfer.dto.TransferStatus;
import org.fl.flowledger.transfer.entity.Transfer;
import org.fl.flowledger.wallet.dto.Currency;
import org.fl.flowledger.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Transfer> findByUuid(UUID transferId);

    List<Transfer> findAllBySenderWallet_User_IdOrReceiverWallet_User_Id(Long userId, Long userId1);

    List<Transfer> findTop50ByStatusOrderByCreatedAtAsc(TransferStatus transferStatus);
}

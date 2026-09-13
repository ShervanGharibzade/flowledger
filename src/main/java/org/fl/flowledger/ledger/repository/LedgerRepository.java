package org.fl.flowledger.ledger.repository;

import org.fl.flowledger.ledger.entity.Ledger;
import org.fl.flowledger.transfer.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerRepository extends JpaRepository<Ledger, Long> {
    List<Ledger> findAllByWalletIdOrderByCreatedAtDesc(UUID walletId);

}

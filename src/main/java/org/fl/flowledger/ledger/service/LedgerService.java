package org.fl.flowledger.ledger.service;

import org.fl.flowledger.ledger.dto.LedgerEntryResponse;

import java.util.List;
import java.util.UUID;

public interface LedgerService {

    List<LedgerEntryResponse> findEntriesForWallet(UUID walletId);

    List<LedgerEntryResponse> findEntriesForTransfer(UUID transferId);
}

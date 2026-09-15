package org.fl.flowledger.ledger.service;

import lombok.RequiredArgsConstructor;
import org.fl.flowledger.auth.service.AuthService;
import org.fl.flowledger.common.exception.TransferNotFoundException;
import org.fl.flowledger.common.exception.UnauthorizedWalletAccessException;
import org.fl.flowledger.common.exception.WalletNotFoundException;
import org.fl.flowledger.ledger.dto.LedgerEntryResponse;
import org.fl.flowledger.ledger.mapper.LedgerMapper;
import org.fl.flowledger.ledger.repository.LedgerRepository;
import org.fl.flowledger.transfer.entity.Transfer;
import org.fl.flowledger.transfer.repository.TransferRepository;
import org.fl.flowledger.wallet.entity.Wallet;
import org.fl.flowledger.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final LedgerRepository ledgerRepository;
    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final AuthService authService;
    private final LedgerMapper ledgerMapper;

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> findEntriesForWallet(UUID walletId) {
        Long userId = authService.getCurrentUserId();

        Wallet wallet = walletRepository
                .findByUuid(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        if (!wallet.getUser().getId().equals(userId)) {
            throw new UnauthorizedWalletAccessException(
                    "You do not have access to this wallet"
            );
        }

        return ledgerMapper.toResponseList(
                ledgerRepository.findAllByWallet_UuidOrderByCreatedAtDesc(walletId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> findEntriesForTransfer(UUID transferId) {
        Long userId = authService.getCurrentUserId();

        Transfer transfer = transferRepository
                .findByUuid(transferId)
                .orElseThrow(() -> new TransferNotFoundException("Transfer not found"));

        boolean owns = transfer.getSenderWallet().getUser().getId().equals(userId)
                || transfer.getReceiverWallet().getUser().getId().equals(userId);

        if (!owns) {
            throw new UnauthorizedWalletAccessException(
                    "You do not have access to this transfer"
            );
        }

        return ledgerMapper.toResponseList(
                ledgerRepository.findAllByTransfer_UuidOrderByCreatedAtDesc(transferId)
        );
    }
}

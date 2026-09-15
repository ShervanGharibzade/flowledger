package org.fl.flowledger.transfer.service;

import lombok.RequiredArgsConstructor;
import org.fl.flowledger.auth.service.AuthService;
import org.fl.flowledger.common.exception.TransferNotFoundException;
import org.fl.flowledger.common.exception.UnauthorizedWalletAccessException;
import org.fl.flowledger.common.exception.WalletNotFoundException;
import org.fl.flowledger.ledger.dto.LedgerType;
import org.fl.flowledger.ledger.entity.Ledger;
import org.fl.flowledger.ledger.repository.LedgerRepository;
import org.fl.flowledger.transfer.dto.CreateTransferDto;
import org.fl.flowledger.transfer.dto.TransferResponse;
import org.fl.flowledger.transfer.dto.TransferStatus;
import org.fl.flowledger.transfer.entity.Transfer;
import org.fl.flowledger.transfer.mapper.TransferMapper;
import org.fl.flowledger.transfer.repository.TransferRepository;
import org.fl.flowledger.wallet.entity.Wallet;
import org.fl.flowledger.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerEntryRepository;
    private final AuthService authService;
    private final TransferMapper transferMapper;
    private final PlatformTransactionManager transactionManager;


    private TransactionTemplate requiresNewTransactionTemplate() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehaviorName("PROPAGATION_REQUIRES_NEW");
        return new TransactionTemplate(transactionManager, def);
    }

    @Transactional
    @Override
    public TransferResponse createTransaction(
            CreateTransferDto dto,
            String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key is required"
            );
        }


        Optional<Transfer> existing = transferRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return transferMapper.toResponse(existing.get());
        }

        Long userId = authService.getCurrentUserId();

        Wallet sender = walletRepository
                .findByUuid(dto.senderWalletId())
                .orElseThrow(() ->
                        new WalletNotFoundException("Sender wallet not found")
                );

        if (!sender.getUser().getId().equals(userId)) {
            throw new UnauthorizedWalletAccessException(
                    "You do not own this wallet"
            );
        }

        Wallet receiver = walletRepository
                .findByUuid(dto.receiverWalletId())
                .orElseThrow(() ->
                        new WalletNotFoundException("Receiver wallet not found")
                );

        if (sender.getUuid().equals(receiver.getUuid())) {
            throw new IllegalArgumentException(
                    "Cannot transfer to the same wallet"
            );
        }

        if (dto.amount() == null ||
                dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }

        if (!sender.getCurrency().equals(receiver.getCurrency())) {
            throw new IllegalArgumentException(
                    "Wallet currencies do not match"
            );
        }

        Transfer transfer = Transfer.builder()
                .senderWallet(sender)
                .receiverWallet(receiver)
                .amount(dto.amount())
                .currency(sender.getCurrency())
                .status(TransferStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        try {
            Transfer saved = transferRepository.save(transfer);
            return transferMapper.toResponse(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {

            return transferRepository.findByIdempotencyKey(idempotencyKey)
                    .map(transferMapper::toResponse)
                    .orElseThrow(() -> e);
        }
    }

    @Transactional
    public void processTransfer(UUID transferId) {

        Transfer transfer = transferRepository
                .findByUuid(transferId)
                .orElseThrow(() ->
                        new TransferNotFoundException("Transfer not found")
                );

        if (transfer.getStatus() != TransferStatus.PENDING) {
            return;
        }

        transfer.setStatus(TransferStatus.PROCESSING);

        UUID senderUuid = transfer.getSenderWallet().getUuid();
        UUID receiverUuid = transfer.getReceiverWallet().getUuid();

        UUID firstUuid = senderUuid.compareTo(receiverUuid) < 0 ? senderUuid : receiverUuid;
        UUID secondUuid = firstUuid.equals(senderUuid) ? receiverUuid : senderUuid;

        Wallet firstLocked = walletRepository
                .findByUuidForUpdate(firstUuid)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
        Wallet secondLocked = walletRepository
                .findByUuidForUpdate(secondUuid)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        Wallet sender = firstUuid.equals(senderUuid) ? firstLocked : secondLocked;
        Wallet receiver = firstUuid.equals(senderUuid) ? secondLocked : firstLocked;

        try {

            if (sender.getBalance()
                    .compareTo(transfer.getAmount()) < 0) {

                transfer.setStatus(TransferStatus.FAILED);

                return;
            }

            sender.setBalance(
                    sender.getBalance()
                            .subtract(transfer.getAmount())
            );


            receiver.setBalance(
                    receiver.getBalance()
                            .add(transfer.getAmount())
            );


            Ledger debit = Ledger.builder()
                    .wallet(sender)
                    .transfer(transfer)
                    .type(LedgerType.DEBIT)
                    .amount(transfer.getAmount())
                    .currency(transfer.getCurrency())
                    .build();

            Ledger credit = Ledger.builder()
                    .wallet(receiver)
                    .transfer(transfer)
                    .type(LedgerType.CREDIT)
                    .amount(transfer.getAmount())
                    .currency(transfer.getCurrency())
                    .build();

            ledgerEntryRepository.save(debit);
            ledgerEntryRepository.save(credit);

            transfer.setStatus(TransferStatus.COMPLETED);
            transfer.setCompletedAt(Instant.now());

        } catch (Exception e) {

            requiresNewTransactionTemplate().executeWithoutResult(status -> {
                Transfer t = transferRepository.findByUuid(transferId).orElse(null);
                if (t != null) {
                    t.setStatus(TransferStatus.FAILED);
                    transferRepository.save(t);
                }
            });

            throw e;
        }
    }


    @Transactional
    public void cancelTransfer(UUID transferId) {

        Transfer transfer = transferRepository
                .findByUuid(transferId)
                .orElseThrow(() ->
                        new TransferNotFoundException("Transfer not found")
                );

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending transfers can be cancelled"
            );
        }

        transfer.setStatus(TransferStatus.CANCELLED);
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse findTransaction(UUID transactionId) {
        Long userId = authService.getCurrentUserId();

        Transfer transfer = transferRepository
                .findByUuid(transactionId)
                .orElseThrow(() -> new TransferNotFoundException("Transfer not found"));

        boolean owns = transfer.getSenderWallet().getUser().getId().equals(userId)
                || transfer.getReceiverWallet().getUser().getId().equals(userId);

        if (!owns) {
            throw new UnauthorizedWalletAccessException(
                    "You do not have access to this transfer"
            );
        }

        return transferMapper.toResponse(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponse> myTransactions() {
        Long userId = authService.getCurrentUserId();

        return transferRepository.findAllBySenderWallet_User_IdOrReceiverWallet_User_Id(
                        userId, userId
                ).stream()
                .map(transferMapper::toResponse)
                .toList();
    }
}
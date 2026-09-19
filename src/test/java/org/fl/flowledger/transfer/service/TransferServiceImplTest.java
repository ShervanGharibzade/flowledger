package org.fl.flowledger.transfer.service;

import org.fl.flowledger.auth.service.AuthService;
import org.fl.flowledger.common.exception.TransferNotFoundException;
import org.fl.flowledger.common.exception.UnauthorizedWalletAccessException;
import org.fl.flowledger.ledger.repository.LedgerRepository;
import org.fl.flowledger.transfer.dto.TransferStatus;
import org.fl.flowledger.transfer.entity.Transfer;
import org.fl.flowledger.transfer.mapper.TransferMapper;
import org.fl.flowledger.transfer.repository.TransferRepository;
import org.fl.flowledger.user.entity.User;
import org.fl.flowledger.wallet.dto.Currency;
import org.fl.flowledger.wallet.entity.Wallet;
import org.fl.flowledger.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private TransferRepository transferRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private LedgerRepository ledgerEntryRepository;
    @Mock
    private AuthService authService;
    @Mock
    private TransferMapper transferMapper;
    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private TransferServiceImpl transferService;

    private User userWithId(Long id) {
        return User.builder().id(id).uuid(UUID.randomUUID()).build();
    }

    private Wallet walletOwnedBy(User owner) {
        return Wallet.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .user(owner)
                .currency(Currency.USD)
                .balance(BigDecimal.ZERO)
                .build();
    }

    private Transfer pendingTransferBetween(Wallet sender, Wallet receiver) {
        return Transfer.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .senderWallet(sender)
                .receiverWallet(receiver)
                .amount(new BigDecimal("25.00"))
                .currency(Currency.USD)
                .status(TransferStatus.PENDING)
                .idempotencyKey("idem-1")
                .reference("TRF-TEST0001")
                .build();
    }

    @Test
    void cancelTransfer_allowsTheSender() {
        User sender = userWithId(1L);
        User receiver = userWithId(2L);
        Transfer transfer = pendingTransferBetween(
                walletOwnedBy(sender), walletOwnedBy(receiver)
        );

        when(authService.getCurrentUserId()).thenReturn(1L);
        when(transferRepository.findByUuid(transfer.getUuid())).thenReturn(Optional.of(transfer));

        transferService.cancelTransfer(transfer.getUuid());

        assertEquals(TransferStatus.CANCELLED, transfer.getStatus());
    }

    @Test
    void cancelTransfer_allowsTheReceiver() {
        User sender = userWithId(1L);
        User receiver = userWithId(2L);
        Transfer transfer = pendingTransferBetween(
                walletOwnedBy(sender), walletOwnedBy(receiver)
        );

        when(authService.getCurrentUserId()).thenReturn(2L);
        when(transferRepository.findByUuid(transfer.getUuid())).thenReturn(Optional.of(transfer));

        transferService.cancelTransfer(transfer.getUuid());

        assertEquals(TransferStatus.CANCELLED, transfer.getStatus());
    }

    @Test
    void cancelTransfer_rejectsAThirdParty() {
        // Regression test: this is the exact IDOR that was fixed - cancelling
        // a transfer you have no part in must be rejected, not silently
        // allowed.
        User sender = userWithId(1L);
        User receiver = userWithId(2L);
        Transfer transfer = pendingTransferBetween(
                walletOwnedBy(sender), walletOwnedBy(receiver)
        );

        when(authService.getCurrentUserId()).thenReturn(999L);
        when(transferRepository.findByUuid(transfer.getUuid())).thenReturn(Optional.of(transfer));

        assertThrows(UnauthorizedWalletAccessException.class,
                () -> transferService.cancelTransfer(transfer.getUuid()));

        assertEquals(TransferStatus.PENDING, transfer.getStatus());
    }

    @Test
    void cancelTransfer_rejectsWhenTransferAlreadyCompleted() {
        User sender = userWithId(1L);
        User receiver = userWithId(2L);
        Transfer transfer = pendingTransferBetween(
                walletOwnedBy(sender), walletOwnedBy(receiver)
        );
        transfer.setStatus(TransferStatus.COMPLETED);

        when(authService.getCurrentUserId()).thenReturn(1L);
        when(transferRepository.findByUuid(transfer.getUuid())).thenReturn(Optional.of(transfer));

        assertThrows(IllegalStateException.class,
                () -> transferService.cancelTransfer(transfer.getUuid()));
    }

    @Test
    void cancelTransfer_throwsTransferNotFound_whenTransferDoesNotExist() {
        UUID missing = UUID.randomUUID();

        when(authService.getCurrentUserId()).thenReturn(1L);
        when(transferRepository.findByUuid(missing)).thenReturn(Optional.empty());

        assertThrows(TransferNotFoundException.class,
                () -> transferService.cancelTransfer(missing));
    }
}

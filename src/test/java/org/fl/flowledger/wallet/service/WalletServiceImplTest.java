package org.fl.flowledger.wallet.service;

import org.fl.flowledger.common.exception.ResourceNotFoundException;
import org.fl.flowledger.common.exception.WalletAccessDeniedException;
import org.fl.flowledger.common.exception.WalletBadRequestException;
import org.fl.flowledger.user.entity.User;
import org.fl.flowledger.user.repository.UserRepository;
import org.fl.flowledger.wallet.Mapper.WalletMapper;
import org.fl.flowledger.wallet.dto.Currency;
import org.fl.flowledger.wallet.dto.CreateWalletDto;
import org.fl.flowledger.wallet.dto.DeleteWalletDto;
import org.fl.flowledger.wallet.dto.WalletResponse;
import org.fl.flowledger.wallet.entity.Wallet;
import org.fl.flowledger.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletServiceImpl walletService;

    private User owner(Long id) {
        return User.builder().id(id).uuid(UUID.randomUUID()).build();
    }

    private Wallet walletOwnedBy(User owner, BigDecimal balance) {
        return Wallet.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .user(owner)
                .currency(Currency.USD)
                .balance(balance)
                .build();
    }


    @Test
    void create_createsWalletForTheAuthenticatedCaller() {
        User caller = owner(1L);
        CreateWalletDto dto = new CreateWalletDto(Currency.USD);

        when(userRepository.findById(1L)).thenReturn(Optional.of(caller));
        when(walletRepository.existsByUserIdAndCurrency(1L, Currency.USD)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletMapper.toResponse(any(Wallet.class)))
                .thenReturn(new WalletResponse(UUID.randomUUID(), Currency.USD, BigDecimal.ZERO, null));

        walletService.create(dto, 1L);
        verify(walletRepository).save(argThat(w -> w.getUser().getId().equals(1L)));
        verify(walletRepository).existsByUserIdAndCurrency(1L, Currency.USD);
    }

    @Test
    void create_rejectsDuplicateCurrencyForSameUser() {
        User caller = owner(1L);
        CreateWalletDto dto = new CreateWalletDto(Currency.USD);

        when(userRepository.findById(1L)).thenReturn(Optional.of(caller));
        when(walletRepository.existsByUserIdAndCurrency(1L, Currency.USD)).thenReturn(true);

        assertThrows(WalletBadRequestException.class, () -> walletService.create(dto, 1L));

        verify(walletRepository, never()).save(any());
    }

    @Test
    void create_throwsResourceNotFound_whenCallerNoLongerExists() {
        CreateWalletDto dto = new CreateWalletDto(Currency.USD);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> walletService.create(dto, 1L));
    }


    @Test
    void remove_deletesWallet_whenCallerIsTheOwnerAndBalanceIsZero() {
        User caller = owner(5L);
        Wallet wallet = walletOwnedBy(caller, BigDecimal.ZERO);
        DeleteWalletDto dto = new DeleteWalletDto(wallet.getUuid());

        when(walletRepository.findByUuid(wallet.getUuid())).thenReturn(Optional.of(wallet));

        String result = walletService.remove(dto, 5L);

        assertNotNull(result);
        verify(walletRepository).delete(wallet);
    }

    @Test
    void remove_rejectsWhenCallerIsNotTheOwner() {
        User actualOwner = owner(5L);
        Wallet wallet = walletOwnedBy(actualOwner, BigDecimal.ZERO);
        DeleteWalletDto dto = new DeleteWalletDto(wallet.getUuid());

        when(walletRepository.findByUuid(wallet.getUuid())).thenReturn(Optional.of(wallet));

        Long someoneElsesId = 999L;

        assertThrows(WalletAccessDeniedException.class,
                () -> walletService.remove(dto, someoneElsesId));

        verify(walletRepository, never()).delete(any());
    }

    @Test
    void remove_rejectsWhenBalanceIsNotZero_regardlessOfOwnership() {
        User caller = owner(5L);
        Wallet wallet = walletOwnedBy(caller, new BigDecimal("10.00"));
        DeleteWalletDto dto = new DeleteWalletDto(wallet.getUuid());

        when(walletRepository.findByUuid(wallet.getUuid())).thenReturn(Optional.of(wallet));

        assertThrows(WalletBadRequestException.class, () -> walletService.remove(dto, 5L));

        verify(walletRepository, never()).delete(any());
    }

    @Test
    void remove_throwsResourceNotFound_whenWalletDoesNotExist() {
        UUID missing = UUID.randomUUID();
        DeleteWalletDto dto = new DeleteWalletDto(missing);

        when(walletRepository.findByUuid(missing)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> walletService.remove(dto, 5L));
    }
}

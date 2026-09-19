package org.fl.flowledger.wallet.service;

import lombok.RequiredArgsConstructor;
import org.fl.flowledger.common.exception.ResourceNotFoundException;
import org.fl.flowledger.common.exception.WalletAccessDeniedException;
import org.fl.flowledger.common.exception.WalletBadRequestException;
import org.fl.flowledger.user.entity.User;
import org.fl.flowledger.user.repository.UserRepository;
import org.fl.flowledger.wallet.Mapper.WalletMapper;
import org.fl.flowledger.wallet.dto.*;
import org.fl.flowledger.wallet.entity.Wallet;
import org.fl.flowledger.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletMapper walletMapper;

    @Override
    @Transactional
    public WalletResponse create(CreateWalletDto dto, Long userId) {

        // The wallet is always created for the authenticated caller - there
        // is no "target user" field on the request, so there's nothing to
        // cross-check against.
        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User", userId)
        );

        boolean walletExists = walletRepository.existsByUserIdAndCurrency(user.getId(), dto.currency());

        if (walletExists) {
            throw new WalletBadRequestException("Wallet already exists for this currency");
        }

        Wallet wallet = Wallet.builder()
                .currency(dto.currency())
                .user(user)
                .build();

        Wallet saved = walletRepository.save(wallet);

        return walletMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public String remove(DeleteWalletDto dto, Long userId) {

        Wallet wallet = walletRepository.findByUuid(dto.walletUuId()).orElseThrow(
                () -> new ResourceNotFoundException("Wallet", dto.walletUuId())
        );

        if (wallet.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new WalletBadRequestException();
        }

        boolean isOwner = wallet.getUser().getId().equals(userId);

        if (!isOwner) {
            throw new WalletAccessDeniedException();
        }

        walletRepository.delete(wallet);

        return "Wallet with this id:%s removed".formatted(wallet.getUuid());
    }

    @Override
    @Transactional
    public WalletResponse findWalletById(UUID walletId,Long userId) {

        return walletMapper.toResponse(walletRepository.findWalletByIdAndUserId(walletId,userId).orElseThrow(
                ()-> new ResourceNotFoundException("Wallet",userId)));
    }

    @Override
    @Transactional
    public List<WalletResponse> findMyWalletsById(Long id) {

        List<Wallet> wallets = walletRepository.findAllByUserId(id);

        return walletMapper.toResponseList(wallets);
    }
}

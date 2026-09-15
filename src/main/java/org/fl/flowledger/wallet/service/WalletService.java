package org.fl.flowledger.wallet.service;

import org.fl.flowledger.wallet.dto.CreateWalletDto;
import org.fl.flowledger.wallet.dto.DeleteWalletDto;
import org.fl.flowledger.wallet.dto.FindWalletDto;
import org.fl.flowledger.wallet.dto.WalletResponse;

import java.util.List;
import java.util.UUID;

public interface WalletService {

    WalletResponse create(CreateWalletDto dto,Long userId);
    String remove(DeleteWalletDto dto,Long userId);
    WalletResponse findWalletById(UUID walletId,Long userId);
    List<WalletResponse> findMyWalletsById(Long id);
}

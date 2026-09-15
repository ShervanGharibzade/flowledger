package org.fl.flowledger.wallet.controller;


import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.fl.flowledger.auth.service.AuthServiceImpl;
import org.fl.flowledger.wallet.dto.CreateWalletDto;
import org.fl.flowledger.wallet.dto.DeleteWalletDto;
import org.fl.flowledger.wallet.dto.WalletResponse;
import org.fl.flowledger.wallet.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

        private final WalletService walletService;
        private final AuthServiceImpl authenticationService;

        @PostMapping
        public ResponseEntity<WalletResponse> create(
                @RequestBody CreateWalletDto dto
        ) {
            Long userId = authenticationService.getCurrentUserId();

            return ResponseEntity.ok(
                    walletService.create(dto, userId)
            );
        }

        @DeleteMapping
        public ResponseEntity<Void> delete(
                @RequestBody DeleteWalletDto dto
        ) {
            Long userId = authenticationService.getCurrentUserId();

            walletService.remove(dto, userId);

            return ResponseEntity.noContent().build();
        }

        @GetMapping
        public ResponseEntity<List<WalletResponse>> findMyWallets() {
            Long userId = authenticationService.getCurrentUserId();

            return ResponseEntity.ok(
                    walletService.findMyWalletsById(userId)
            );
        }

        @GetMapping("/Wallets/{id}")
        public ResponseEntity<WalletResponse> findWalletById(
                @PathVariable UUID id
        ) {
            Long userId = authenticationService.getCurrentUserId();

            return ResponseEntity.ok(
                    walletService.findWalletById(id,userId)
            );
        }
}

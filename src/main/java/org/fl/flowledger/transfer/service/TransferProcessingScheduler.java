package org.fl.flowledger.transfer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fl.flowledger.transfer.dto.TransferStatus;
import org.fl.flowledger.transfer.entity.Transfer;
import org.fl.flowledger.transfer.repository.TransferRepository;
import org.fl.flowledger.transfer.service.TransferService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class TransferProcessingScheduler {

    private static final int BATCH_SIZE = 50;

    private final TransferRepository transferRepository;
    private final TransferService transferService;

    @Scheduled(fixedDelay = 5000)
    public void pollAndProcessPendingTransfers() {

        List<Transfer> pending = transferRepository
                .findTop50ByStatusOrderByCreatedAtAsc(TransferStatus.PENDING);

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Picked up {} pending transfer(s) to process", pending.size());

        for (Transfer transfer : pending) {
            UUID id = transfer.getUuid();
            try {
                transferService.processTransfer(id);
            } catch (Exception e) {
                log.error("Failed to process transfer {}", id, e);
            }
        }
    }
}
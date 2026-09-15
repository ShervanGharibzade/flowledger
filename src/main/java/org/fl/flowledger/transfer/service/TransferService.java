package org.fl.flowledger.transfer.service;

import org.fl.flowledger.transfer.dto.CreateTransferDto;
import org.fl.flowledger.transfer.dto.TransferResponse;

import java.util.List;
import java.util.UUID;



public interface TransferService {

    TransferResponse createTransaction(CreateTransferDto dto, String idempotencyKey);

    void processTransfer(UUID transferId);

    void cancelTransfer(UUID transferId);

    TransferResponse findTransaction(UUID transactionId);

    List<TransferResponse> myTransactions();
}
package org.fl.flowledger.ledger.mapper;

import org.fl.flowledger.ledger.dto.LedgerEntryResponse;
import org.fl.flowledger.ledger.entity.Ledger;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface LedgerMapper {

    @Mapping(target = "id", source = "uuid")
    @Mapping(target = "walletId", source = "wallet.uuid")
    @Mapping(target = "transferId", source = "transfer.uuid")
    LedgerEntryResponse toResponse(Ledger ledger);

    List<LedgerEntryResponse> toResponseList(List<Ledger> ledgers);
}

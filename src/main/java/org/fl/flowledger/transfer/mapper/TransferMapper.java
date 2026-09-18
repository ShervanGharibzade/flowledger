package org.fl.flowledger.transfer.mapper;


import org.fl.flowledger.transfer.dto.TransferResponse;
import org.fl.flowledger.transfer.entity.Transfer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TransferMapper {

    @Mapping(
            target = "receiverWallet",
            source = "receiverWallet.uuid"
    )
    TransferResponse toResponse(Transfer transfer);
}

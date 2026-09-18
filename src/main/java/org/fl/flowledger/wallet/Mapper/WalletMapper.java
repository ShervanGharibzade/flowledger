package org.fl.flowledger.wallet.Mapper;

import org.fl.flowledger.wallet.dto.WalletResponse;
import org.fl.flowledger.wallet.entity.Wallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface WalletMapper {

    @Mapping(target = "id", source = "uuid")
    WalletResponse toResponse(Wallet wallet);
    List<WalletResponse> toResponseList(List<Wallet> wallets);
}

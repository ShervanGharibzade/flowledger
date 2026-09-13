package org.fl.flowledger.auth.AuthMapper;

import org.fl.flowledger.auth.dto.LoginResponse;
import org.fl.flowledger.auth.dto.LoginResult;
import org.fl.flowledger.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AuthMapper {
    LoginResponse toResponse(LoginResult data);
    LoginResult toData(LoginResult data);
}

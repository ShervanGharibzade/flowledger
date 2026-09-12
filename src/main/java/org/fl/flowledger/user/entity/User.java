package org.fl.flowledger.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.fl.flowledger.common.entity.BaseEntity;
import org.fl.flowledger.user.dto.UserRoles;
import org.fl.flowledger.user.dto.UserStatus;


@Entity
@Table(name = "Users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"passwordHash","role"})
public class User extends BaseEntity {

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255,name = "passord_hash")
    private String passwordHash;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private UserRoles role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private UserStatus status;
}

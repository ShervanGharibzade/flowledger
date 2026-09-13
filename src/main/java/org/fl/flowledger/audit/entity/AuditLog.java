package org.fl.flowledger.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.fl.flowledger.audit.dto.AuditAction;
import org.fl.flowledger.audit.dto.AuditEntityType;
import org.fl.flowledger.common.entity.BaseEntity;
import org.fl.flowledger.user.entity.User;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(exclude = {"user"})
public class AuditLog extends BaseEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 100)
    private AuditEntityType entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;

}
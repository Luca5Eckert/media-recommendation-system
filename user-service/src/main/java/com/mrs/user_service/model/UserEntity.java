package com.mrs.user_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;

    private String email;

    private String password;

    private String fullName;

    @Builder.Default
    private boolean active = true;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createAt;

    @UpdateTimestamp
    private Instant updateAt;

    private Instant deletedAt;

}
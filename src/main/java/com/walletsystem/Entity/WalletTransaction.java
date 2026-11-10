package com.walletsystem.Entity;

import jakarta.persistence.*;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_wallet_idempotency", columnNames = {"wallet_id", "idempotency_key"}),
        indexes = @Index(name = "idx_tx_wallet_created", columnList = "wallet_id, created_at")
)
@Getter
@Setter
@NoArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "wallet_id",nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 2)
    private TransactionType type;

    @Column(nullable = false,precision = 19,scale = 2)
    private BigDecimal amount;

    @Column(nullable = false,precision = 19,scale = 2)
    private  BigDecimal balanceAfter;

    @Column(nullable = false,length = 36)
    private String reference;

    private  Long counterpartyWalletId;

    @Column(length = 255)
    private String description;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

  @PrePersist
    void onCreate(){
        createdAt=LocalDateTime.now();
    }
}

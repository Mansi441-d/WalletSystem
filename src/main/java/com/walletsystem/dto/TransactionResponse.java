package com.walletsystem.dto;

import com.walletsystem.Entity.TransactionType;
import com.walletsystem.Entity.WalletTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String reference,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Long counterpartyWalletId,
        String description,
        LocalDateTime createdAt
) {
    public  static TransactionResponse from(WalletTransaction t){
        return new TransactionResponse(
                t.getId(),
                t.getReference(),
                t.getType(),
                t.getAmount(),
                t.getBalanceAfter(),
                t.getCounterpartyWalletId(),
                t.getDescription(),
                t.getCreatedAt()
        );
    }
}

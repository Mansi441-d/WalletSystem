package com.walletsystem.dto;

import com.walletsystem.Entity.Wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        Long walletId,
        String ownerName,
        String ownEmail,
        BigDecimal balance,
        LocalDateTime createAt
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUser().getFullName(),
                wallet.getUser().getEmail(),
                wallet.getBalance(),
                wallet.getCreatedAt());
    }
}

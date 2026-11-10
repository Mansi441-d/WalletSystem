package com.walletsystem.dto;

import java.math.BigDecimal;

public record LedgerSummaryResponse(
        BigDecimal totalWalletBalance,
        BigDecimal totalDeposits,
        BigDecimal totalWithdrawals,
        BigDecimal expectedBalance,
        boolean consistent
) {
}

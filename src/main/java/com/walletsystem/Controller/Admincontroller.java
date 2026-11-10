package com.walletsystem.Controller;

import com.walletsystem.dto.LedgerSummaryResponse;
import com.walletsystem.dto.PageResponse;
import com.walletsystem.dto.TransactionResponse;
import com.walletsystem.dto.WalletResponse;
import com.walletsystem.service.WalletService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin")
public class Admincontroller {

    private final WalletService walletService;
    @GetMapping("/wallets")
    public PageResponse<WalletResponse> wallets(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return walletService.listWallets(page, size);
    }

    @GetMapping("/wallets/{walletId}/transactions")
    public PageResponse<TransactionResponse> walletTransactions(@PathVariable Long walletId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        return walletService.walletTransactions(walletId, page, size);
    }

    @GetMapping("/ledger/summary")
    public LedgerSummaryResponse ledgerSummary() {
        return walletService.ledgerSummary();
    }

}

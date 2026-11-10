package com.walletsystem.Controller;


import com.walletsystem.dto.*;
import com.walletsystem.service.WalletService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Wallet")
public class WalletController {
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final WalletService walletService;

    @GetMapping
    public WalletResponse myWallet(Authentication authentication) {
        return walletService.getWallet(authentication.getName());
    }

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse deposit(Authentication authentication,
                                       @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                                       @Valid @RequestBody AmountRequest request) {
        return walletService.deposit(authentication.getName(), request.amount(), request.description(), idempotencyKey);
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse withdraw(Authentication authentication,
                                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                                        @Valid @RequestBody AmountRequest request) {
        return walletService.withdraw(authentication.getName(), request.amount(), request.description(), idempotencyKey);
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(Authentication authentication,
                                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                                        @Valid @RequestBody TransferRequest request) {
        return walletService.transfer(authentication.getName(), request.toEmail(), request.amount(),
                request.description(), idempotencyKey);
    }

    @GetMapping("/transactions")
    public PageResponse<TransactionResponse> history(Authentication authentication,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return walletService.history(authentication.getName(), page, size);
    }
}

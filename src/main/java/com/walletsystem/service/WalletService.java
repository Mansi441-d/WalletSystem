package com.walletsystem.service;

import com.walletsystem.Entity.TransactionType;
import com.walletsystem.Entity.Wallet;
import com.walletsystem.Entity.WalletTransaction;
import com.walletsystem.Exception.BadRequestException;
import com.walletsystem.Exception.InsufficientBalanceException;
import com.walletsystem.Exception.ResourceNotFoundException;
import com.walletsystem.dto.LedgerSummaryResponse;
import com.walletsystem.dto.PageResponse;
import com.walletsystem.dto.TransactionResponse;
import com.walletsystem.dto.WalletResponse;
import com.walletsystem.repository.WalletRepository;
import com.walletsystem.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {

    private static final int MAX_KEY_LENGTH = 100;

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    // ---------------------------------------------------------------- user operations

    @Transactional(readOnly = true)
    public WalletResponse getWallet(String email) {
        Wallet wallet = walletRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        return WalletResponse.from(wallet);
    }

    @Transactional
    public TransactionResponse deposit(String email, BigDecimal amount, String description, String idempotencyKey) {
        String key = cleanKey(idempotencyKey);
        BigDecimal value = scale(amount);

        Wallet wallet = lockWallet(walletIdOf(email));

        Optional<WalletTransaction> replay = findReplay(wallet.getId(), key);
        if (replay.isPresent()) {
            return TransactionResponse.from(replay.get());
        }

        wallet.setBalance(wallet.getBalance().add(value));
        WalletTransaction tx = saveEntry(wallet, TransactionType.DEPOSIT, value, null,
                UUID.randomUUID().toString(), description, key);
        return TransactionResponse.from(tx);
    }

    @Transactional
    public TransactionResponse withdraw(String email, BigDecimal amount, String description, String idempotencyKey) {
        String key = cleanKey(idempotencyKey);
        BigDecimal value = scale(amount);

        Wallet wallet = lockWallet(walletIdOf(email));

        Optional<WalletTransaction> replay = findReplay(wallet.getId(), key);
        if (replay.isPresent()) {
            return TransactionResponse.from(replay.get());
        }

        if (wallet.getBalance().compareTo(value) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(value));
        WalletTransaction tx = saveEntry(wallet, TransactionType.WITHDRAWAL, value, null,
                UUID.randomUUID().toString(), description, key);
        return TransactionResponse.from(tx);
    }

    @Transactional
    public TransactionResponse transfer(String fromEmail, String toEmail, BigDecimal amount,
                                        String description, String idempotencyKey) {
        String key = cleanKey(idempotencyKey);
        BigDecimal value = scale(amount);
        String receiverEmail = toEmail.trim().toLowerCase(Locale.ROOT);

        if (fromEmail.equalsIgnoreCase(receiverEmail)) {
            throw new BadRequestException("You cannot transfer money to yourself");
        }

        Long fromId = walletIdOf(fromEmail);
        Long toId = walletIdOf(receiverEmail, "Receiver not found");

        // Always lock the lower id first. Two opposite transfers (A to B and B to A) then
        // acquire locks in the same order, so they cannot deadlock each other.
        Long firstId = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);
        Wallet first = lockWallet(firstId);
        Wallet second = lockWallet(secondId);
        Wallet from = fromId.equals(firstId) ? first : second;
        Wallet to = fromId.equals(firstId) ? second : first;

        Optional<WalletTransaction> replay = findReplay(from.getId(), key);
        if (replay.isPresent()) {
            return TransactionResponse.from(replay.get());
        }

        if (from.getBalance().compareTo(value) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(value));
        to.setBalance(to.getBalance().add(value));

        String reference = UUID.randomUUID().toString();
        WalletTransaction out = saveEntry(from, TransactionType.TRANSFER_OUT, value, to.getId(), reference, description, key);
        saveEntry(to, TransactionType.TRANSFER_IN, value, from.getId(), reference, description, null);
        return TransactionResponse.from(out);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> history(String email, int page, int size) {
        Long walletId = walletIdOf(email);
        return PageResponse.from(transactionRepository
                .findByWalletIdOrderByCreatedAtDescIdDesc(walletId, pageable(page, size, Sort.unsorted()))
                .map(TransactionResponse::from));
    }

    // ---------------------------------------------------------------- admin operations

    @Transactional(readOnly = true)
    public PageResponse<WalletResponse> listWallets(int page, int size) {
        return PageResponse.from(walletRepository
                .findAll(pageable(page, size, Sort.by("id")))
                .map(WalletResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> walletTransactions(Long walletId, int page, int size) {
        if (!walletRepository.existsById(walletId)) {
            throw new ResourceNotFoundException("Wallet not found");
        }
        return PageResponse.from(transactionRepository
                .findByWalletIdOrderByCreatedAtDescIdDesc(walletId, pageable(page, size, Sort.unsorted()))
                .map(TransactionResponse::from));
    }

    @Transactional(readOnly = true)
    public LedgerSummaryResponse ledgerSummary() {
        BigDecimal balances = orZero(walletRepository.totalBalance());
        BigDecimal deposits = orZero(transactionRepository.sumAmountByType(TransactionType.DEPOSIT));
        BigDecimal withdrawals = orZero(transactionRepository.sumAmountByType(TransactionType.WITHDRAWAL));
        BigDecimal expected = deposits.subtract(withdrawals);
        return new LedgerSummaryResponse(balances, deposits, withdrawals, expected, balances.compareTo(expected) == 0);
    }

    // ---------------------------------------------------------------- helpers

    private Long walletIdOf(String email) {
        return walletIdOf(email, "Wallet not found");
    }

    private Long walletIdOf(String email, String notFoundMessage) {
        return walletRepository.findIdByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
    }

    private Wallet lockWallet(Long walletId) {
        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private Optional<WalletTransaction> findReplay(Long walletId, String key) {
        if (key == null) {
            return Optional.empty();
        }
        return transactionRepository.findByWalletIdAndIdempotencyKey(walletId, key);
    }

    private WalletTransaction saveEntry(Wallet wallet, TransactionType type, BigDecimal amount,
                                        Long counterpartyWalletId, String reference,
                                        String description, String idempotencyKey) {
        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(wallet.getBalance());
        tx.setReference(reference);
        tx.setCounterpartyWalletId(counterpartyWalletId);
        tx.setDescription(description);
        tx.setIdempotencyKey(idempotencyKey);
        return transactionRepository.save(tx);
    }

    private String cleanKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String trimmed = key.trim();
        if (trimmed.length() > MAX_KEY_LENGTH) {
            throw new BadRequestException("Idempotency-Key must be at most " + MAX_KEY_LENGTH + " characters");
        }
        return trimmed;
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value;
    }

    private Pageable pageable(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
    }
}

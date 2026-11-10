package com.walletsystem.repository;

import com.walletsystem.Entity.TransactionType;
import com.walletsystem.Entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDescIdDesc(Long walletId, Pageable pageable);
    Optional<WalletTransaction> findByWalletIdAndIdempotencyKey(Long walletId, String idempotencyKey);

    @Query("select sum(t.amount) from WalletTransaction t where t.type = :type")
    BigDecimal sumAmountByType(@Param("type")TransactionType type);
}

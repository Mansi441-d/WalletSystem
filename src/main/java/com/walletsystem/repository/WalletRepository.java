package com.walletsystem.repository;

import com.walletsystem.Entity.Wallet;
import jakarta.persistence.Entity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;


public interface WalletRepository extends JpaRepository <Wallet,Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") Long id);

    @Query("select w.id from Wallet w where w.user.email = :email")
    Optional<Long> findIdByUserEmail(@Param("email") String email);

    Optional<Wallet> findByUserEmail(String email);

    @Query("select sum(w.balance) from Wallet w")
    BigDecimal totalBalance();

    @Override
    @EntityGraph(attributePaths = "user")
    Page<Wallet>findAll(Pageable pageable);



}

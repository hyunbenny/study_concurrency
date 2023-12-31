package com.hyunbenny.study_concurrency.repository;

import com.hyunbenny.study_concurrency.domain.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface StockRepository extends JpaRepository<Stock, Long> {

    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock  s WHERE s.id = :id")
    Stock findByIdWithPessimisticLock(Long id);

}

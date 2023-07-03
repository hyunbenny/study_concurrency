package com.hyunbenny.study_concurrency.repository;

import com.hyunbenny.study_concurrency.domain.OptimisticLockStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OptimisticLockStockRepository extends JpaRepository<OptimisticLockStock, Long> {

    @Lock(value = LockModeType.OPTIMISTIC)
    @Query("SELECT s FROM OptimisticLockStock  s WHERE s.id = :id")
    OptimisticLockStock findByIdWithOptimisticLock(Long id);

}

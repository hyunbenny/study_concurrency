package com.hyunbenny.study_concurrency.service;

import com.hyunbenny.study_concurrency.domain.OptimisticLockStock;
import com.hyunbenny.study_concurrency.repository.OptimisticLockStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptimisticLockStockService {

    private final OptimisticLockStockRepository stockRepository;

    @Transactional
    public void decrease(Long id, Long quantity) {
        OptimisticLockStock stock = stockRepository.findByIdWithOptimisticLock(id);

        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }
}

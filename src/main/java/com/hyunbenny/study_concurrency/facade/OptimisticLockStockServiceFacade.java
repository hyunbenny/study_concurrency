package com.hyunbenny.study_concurrency.facade;

import com.hyunbenny.study_concurrency.service.OptimisticLockStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OptimisticLockStockServiceFacade {

    private final OptimisticLockStockService optimisticLockStockService;

    public void decrease(long id, long quantity) throws InterruptedException {

        while (true) {
            try {
                optimisticLockStockService.decrease(id, quantity);
                break;
            } catch (Exception e) {
                Thread.sleep(50);
            }
        }
    }

}

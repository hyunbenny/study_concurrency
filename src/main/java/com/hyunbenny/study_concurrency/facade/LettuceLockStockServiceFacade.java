package com.hyunbenny.study_concurrency.facade;

import com.hyunbenny.study_concurrency.repository.RedisLettuceLockRepository;
import com.hyunbenny.study_concurrency.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LettuceLockStockServiceFacade {

    private final RedisLettuceLockRepository redisRepository;
    private final StockService stockService;

    public void decrease(Long id, Long quantity) throws InterruptedException {
        while (!redisRepository.lock(id)) {
            Thread.sleep(100); // Redis에 많은 부하가 가는 것을 막기 위해서 `Thread.sleep()`을 통해 부하를 좀 줄여주자.
        }

        try {
            stockService.decrease(id, quantity);
        } finally{
            redisRepository.unlock(id);
        }

    }
}

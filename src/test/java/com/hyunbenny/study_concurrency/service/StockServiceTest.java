package com.hyunbenny.study_concurrency.service;

import com.hyunbenny.study_concurrency.Stock;
import com.hyunbenny.study_concurrency.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StockServiceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void before() {
        Stock stock = new Stock(1L, 100L);
        stockRepository.saveAndFlush(stock);
    }

    @AfterEach
    void after() {
        stockRepository.deleteAll();
    }

    @Test
    @Disabled
    void stock_decrease() {
        stockService.decrease(1L, 1L);

        Stock stock = stockRepository.findById(1L).orElseThrow();
        assertEquals(99, stock.getQuantity());
    }

    @Test
    void stock_decrease_100request_atTheSameTime() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        // CountDownLatch : 다른 스레드에서 진행 중인 작업이 완료될 때까지 대기할 수 있도록 도와주는 클래스

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try{
                    stockService.decrease(1L, 1L);
                }finally{
                    latch.countDown();
                }
            });
        }

        latch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow();

        // @Transactional이 붙은 상태에서는 Race Condition으로 인해 예상과 다른 결과가 나온다.
        assertEquals(0L, stock.getQuantity());
    }



}
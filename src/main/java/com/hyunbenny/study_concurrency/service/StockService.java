package com.hyunbenny.study_concurrency.service;

import com.hyunbenny.study_concurrency.domain.Stock;
import com.hyunbenny.study_concurrency.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    /**
     * 메서드 선언부에 synchronized를 붙여주면 하나의 쓰레드만 접근가능하다.
     * 하지만 트랜잭션이 완료되고 commit전에 다른 쓰레드에서 decrease()를 호출하여 수정 전의 데이터를 가져가기 때문에 의도했던대로 동작하지 않는다.
     * -> @Transactional을 주석처리하고 테스트 케이스를 돌리면 정상적으로 동작한다.
     *
     * synchronized를 사용할 때의 문제점
     * - 하나의 프로세스 안에서만 보장되기 때문에 실무와 같이 서버가 여러대 있는 경우에는 동시성을 보장하지 않는다.
     */
//    @Transactional
    public synchronized void decrease(Long id, Long quantity) {
        // get stock entity
        Stock stock = stockRepository.findById(id).orElseThrow();

        // decrease stock
        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);
    }
}

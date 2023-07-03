package com.hyunbenny.study_concurrency.facade;

import com.hyunbenny.study_concurrency.repository.NamedLockRepository;
import com.hyunbenny.study_concurrency.service.NamedLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NamedLockStockServiceFacade {

    private final NamedLockService namedLockService;
    private final NamedLockRepository namedLockRepository;

    @Transactional
    public void decrease(Long id, Long quantity) {
        try {
            namedLockRepository.getLock(String.valueOf(id));
            namedLockService.decrease(id, quantity);
        }finally {
            namedLockRepository.releaseLock(String.valueOf(id));
        }


    }

}

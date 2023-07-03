package com.hyunbenny.study_concurrency.repository;

import com.hyunbenny.study_concurrency.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {


}

package com.hyunbenny.study_concurrency.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class OptimisticLockStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private Long quantity;

    @Version
    private Long version;

    public OptimisticLockStock() {
    }

    public OptimisticLockStock(Long productId, Long quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void decrease(Long quantity) {
        if(this.quantity - quantity < 0) throw new RuntimeException("수량은 0개 미만이 될 수 없습니다.");
        this.quantity = this.quantity - quantity;
    }
}

package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    @Autowired
    private StockRepository stockRepository;

    //@Transactional
    public synchronized void decrease(Long id, Long quantity){
        Stock stock = stockRepository.findById(id).orElseThrow();

        stock.decrease(quantity);

        stockRepository.save(stock);
    }

    /**
     * NamedLock으로 인한
     * 부모의 트랜잭션과 별도로 실행하기 위해  Propagation.REQUIRES_NEW 적용
     * */

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decreaseWithNamedLock(Long id, Long quantity){
        Stock stock = stockRepository.findById(id).orElseThrow();

        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);
    }
}

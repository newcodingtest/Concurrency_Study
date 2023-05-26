package com.example.stock.facade;

import com.example.stock.repository.LockRepository;
import com.example.stock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NamedLockStockFacade {

    @Autowired
    private LockRepository lockRepository;
    @Autowired
    private StockService stockService;

    @Transactional
    public void decrease(Long id, Long quantity)  {
            try{
                lockRepository.getLock(id.toString());
                stockService.decreaseWithNamedLock(id,quantity);
            }finally {
                lockRepository.releaseLock(id.toString());
            }
        }


}

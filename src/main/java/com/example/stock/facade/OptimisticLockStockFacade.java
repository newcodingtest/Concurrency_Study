package com.example.stock.facade;

import com.example.stock.service.OptimisticLockStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class OptimisticLockStockFacade {

    @Autowired
    private OptimisticLockStockService optimisticLockService;

    /**
     * OptimisticLock 은 실패시 재시도가 필요함으로 while break 문 적용
     * */

    public void decrease(Long id, Long quantity) throws InterruptedException {
        while (true){
            try{
                optimisticLockService.decrease(id,quantity);

                break;
            }catch (Exception e){
                Thread.sleep(50);
            }
        }
    }
}

package com.example.stock.facade;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
@SpringBootTest
public class OptimisticLockStockFacadeTest {
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private OptimisticLockStockFacade optimisticLockService;
    @BeforeEach
    public void before(){
        Stock stock = new Stock(1L, 100L);

        stockRepository.save(stock);
    }

    @AfterEach
    public void after(){
        stockRepository.deleteAll();
    }

    @Test
    public void Optimistic_락을_적용하여_동시에_100개의_요청() throws InterruptedException {
        int threadCnt = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCnt);


        for (int i=0; i< threadCnt; i++){
            executorService.submit(() -> {
                try{
                    optimisticLockService.decrease(1l,1l);
                }catch (InterruptedException e){
                    throw new RuntimeException(e);
                }finally {
                    latch.countDown();
                }

            });
        }
        latch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow();

        assertEquals(0L,stock.getQuantity());
    }
}

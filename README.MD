
1. synchronized 는 하나의 프로세서(하나의 서버) 에서만 유효하다. 2개 이상의 서버일 경우 동시성 이슈가 발생한다.

1의 문제를 해결 해결하기 위한 방법은??

1. MySql Lock
    1. Pesimistic Lock
    2. Optimistic Lock
    3. Named Lock


**1. Pessimistic Lock**

- 실제로 데이터에 Lock을 걸어서 정합성을 맞추는 방법입니다.
- exclusive lock(베타적 잠금) 을 걸게되면 다른 트랜잭션에서는 lock 이 해제되기전에 데이터를 가져갈 수 없게됩니다.
- ✨ 자원 요청에 따른 **동시성문제가 발생할 것이라고 예상하고 락**을 걸어버리는 **비관적 락** 방식입니다.
- 하지만, 데드락이 걸릴 수 있기 때문에 주의하여 사용해야합니다.

![](https://blog.kakaocdn.net/dn/6oWmB/btrNviyzlKp/oo6trLHChrh96LQIyz3UlK/img.png)

Pessimistic Lock

예를들어 Server 1 DB 데이터를 가져올 떄, Lock 을 걸어버리면, 다른 서버에서는 Server1의 작업이 끝나 락이 풀릴 때 까지, 데이터에 접근하지 못하게 됩니다.

- 장점
    - 충돌이 빈번히 일어나면 Optimistic Lock보다 성능이 좋음
    - 락을 통한 update인한 데이터 정합성 보장

- 단점
    - 별도의 lock을 점유하고 있기에 성능 문제 발생 가능성이 존


**서비스**
```
@Service  
public class PessimisticLockStockService {  
  
@Autowired  
private StockRepository stockRepository;  
  
	public void decrease(Long id, Long quantity){  
		Stock stock = stockRepository.findByIdWithPessimisticLock(id);  
		  
		stock.decrease(quantity);  
		  
		stockRepository.saveAndFlush(stock);  
	}  
}
```

**레포지토리**
```
@Repository  
public interface StockRepository extends JpaRepository<Stock,Long> {  
  
/**  
* 비관적 락(Pessimistic Lock)  
*  
*/  
@Lock(value = LockModeType.PESSIMISTIC_WRITE)  
@Query("select s from Stock s where s.id = :id")  
Stock findByIdWithPessimisticLock(@Param("id") final Long id);  
}```


```


**테스트 코드**
```
@Test  
public void 비관적_락을_적용하여_동시에_100개의_요청() throws InterruptedException {  
	int threadCnt = 100;  
	  
	ExecutorService executorService = Executors.newFixedThreadPool(32);  
	CountDownLatch latch = new CountDownLatch(threadCnt);  
	  
	  
	for (int i=0; i< threadCnt; i++){  
		executorService.submit(() -> {  
			try{  
				pessimisticLockStockService.decrease(1l,1l);  
			}finally {  
				latch.countDown();  
			}  
		  
		});  
	}  
	latch.await();  
	  
	Stock stock = stockRepository.findById(1L).orElseThrow();  
	  
	assertEquals(0L,stock.getQuantity());  
}
```


**2. Optimisitc Lock**

- 실제로 Lock 을 이용하지 않고 **버전**을 이용함으로써 정합성을 맞추는 방법입니다.
- 먼저 데이터를 읽은 후에 update 를 수행할 떄 현재 **내가 읽은 버전이 맞는지 확인**하며 업데이트 합니다.
- ✨ 자원에 락을 걸어서 선점하지 않고, 동시성 문제가 발생하면 그때가서 처리하는 **낙관적 락** 방식입니다.
- 내가 읽은 버전에서 수정사항이 생겼을 경우에는 application에서 다시 읽은 후에 작업을 수행하는 롤백 작업을 수행해야 합니다.

[과정]

1) 서버 1이 version1 임을 조건절에 명시하면서 업데이트 쿼리를 날립니다.

2) version1 쿼리가 업데이트 되어서, 디비는 version 2가 됩니다.

![](https://blog.kakaocdn.net/dn/dKqfxu/btrNvi6tcWA/VY4ayDO2LtYNK0I13Re1PK/img.png)

Optimisitc Lock 점유 과정

3) server2 가 version1 로 업데이트 쿼리를 날리면 버전이 맞지않아 실패합니다.

4) 쿼리가 실패하면 server2 에서 다시 조회하여 버전을 맞춘 후 업데이트 쿼리를 날리는 과정을 거칩니다.

![](https://blog.kakaocdn.net/dn/ldJlh/btrNuRuEfAz/KzxNdqnBgIEykORQzUkOhk/img.png)

Optimisitc Lock 점유 과정


- 장점
    - 별도의 Lock 을 점유하지 않으므로 성능상의 이점이 있다.

- 단점
- update 실패시 재시도 로직을 개발자가 작성해야함
- 충돌이 빈번하면 Pessimistic Lock에 비해 성능이 떨어질 수도 있다.

**서비스**
```
@Service  
public class OptimisticLockStockService {  
  
@Autowired  
private StockRepository stockRepository;  
  
@Transactional  
public void decrease(Long id, Long quantity){  
	Stock stock = stockRepository.findByIdWithOptimisticLock(id);  
	  
	stock.decrease(quantity);  
	  
	stockRepository.saveAndFlush(stock);  
}  
  
}
```

**퍼사드**
```
@Service  
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
```
**레포지토리**
```
@Repository  
public interface StockRepository extends JpaRepository<Stock,Long> {  
	/**  
	* 비관적 락(Pessimistic Lock)  
	*  
	*/  
	@Lock(value = LockModeType.OPTIMISTIC)  
	@Query("select s from Stock s where s.id = :id")  
	Stock findByIdWithOptimisticLock(@Param("id") final Long id);  
}
```

**테스트 코드**
```
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
```


**2. Named Lock**

- Named Lock은 이름을 가진 metadata Lock 입니다.
- 이름을 가진 락을 획득한 후, 해지될때 까지 다른 세션은 이 락을 획득할 수 없게 됩니다.
- 주의할 점은, 트랜잭션이 종료될 떄 락이 자동으로 해지되지 않기 떄문에, 별도로 해지해주거나 선점시간이 끝나야 해지됩니다.
- Mysql 에서는 getLock( ) 을 통해 획들 / releaseLock() 으로 해지 할 수 있습니다.

[Named lock 시 Lock 점유 과정]

1. Named Lock은 Stock에 락을 걸지 않고, 별도의 공간에 락을 겁니다.
2. session-1 이 1이라는 이름으로 락을 건다면, session 1 이 1을 해지한 후에 락을 얻을 수 있습니다.

![](https://blog.kakaocdn.net/dn/mZ7OR/btrNuoG3FK5/i0JEKOPVKNQon8sUgc29s1/img.png)

Named Lock 점유 과정

_⚡️ Named Lock 사용시 주의사항_

- 예제에서는 동일한 DataSource 를 사용하지만, 실제 서비스에서는 커넥션풀이 부족해질 수 있기에 DataSoruce 를 분리하는 걸 추천한다고 합니다.

**🖥LockRepository** 

- 예제에서는 편의성을 위해서 Stock 엔티티를 사용하지만, 실무에서는 별도의 JDBC 를 사용해야 한다고 합니다.

```
@Repository
public interface LockRepository extends JpaRepository<Stock,Long> {
    @Query(value = "select get_lock(:key,3000)", nativeQuery = true)
    void getLock(String key);

    @Query(value = "select release_lock(:key)", nativeQuery = true)
    void releaseLock(String key);

}
```

**🖥 NamedLockFacde**

- StockService 는 부모의 트랜잭션과 별도로 실행되어야하기 때문에 propergation을 별도로 생성해줍니다
- 부모의 트랜잭션과 동일한 범위로 묶인다면 Synchronized 와 같은 문제인 DataBase에 commit 되기전에 락이 풀리는 현상이 발생합니다.
- 그렇기 때문에 별도의 트랜잭션으로 분리해서 DataBase에 정상적으로 Commit이 된 후에 락을 해제해 주려는 의도르 품고있다고합니다.
- 핵심은 Lock을 해제하기전에 DataBase에 Commit이 되도록 하는것..!!

```
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
```

락을 획득한 후, 비지니스 로직을 처리합니다.

그 후 finally 에서 락을 해지해줍니다.

그리구 예제에서는 같은 DataSource 를 사용해주어야하기 때문에 커넥션 풀 수를 늘려주어야 합니다.

```
spring:
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: true
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/stock_example
    username: root
    password: root
    hikari:
      maximum-pool-size: 40
logging:
  level:
    org:
      hibernate:
        SQL: DEBUG
        type:
          descriptor:
            sql:
              BasicBinder: TRACE
```

🖥 **NamedFacadeTest**

```
@SpringBootTest
public class NamedLockStockFacadeTest {
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private NamedLockStockFacade namedLockStockFacade;
    @BeforeEach
    public void before(){
        Stock stock = new Stock(1L, 100L);

        stockRepository.saveAndFlush(stock);
    }

    @AfterEach
    public void after(){
        stockRepository.deleteAll();
    }

    @Test
    public void 네임드락_을_이용한_동시에_100개의_요청() throws InterruptedException {
        int threadCnt = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(threadCnt);


        for (int i=0; i< threadCnt; i++){
            executorService.submit(() -> {
                try{
                    namedLockStockFacade.decrease(1l,1l);
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
```

**📙 Named Lock 이 장점**

- 📌 NamedLock 은 주로 분산락을 구현할 때 사용합니다.
- Pessimistic 락은 time out을 구현하기 굉장히 힘들지만, Named Lock은 비교적 손쉽게 구현할 수 있다고 합니다.
- 그 외에, 데이터 정합성을 맞춰야하는 경우에도 사용할 수 있다고 합니다.

**📘 Named Lock 이 단점**

- 하지만., Naemd Lock 은 트랜잭션 종료 시에, 락 해제와 세션관리를 잘 해주어야하므로 주의해서 사용주어야 합니다.
- 또 실제 사용할 때는 구현방법이, 복잡할 수 있습니다.

<br>
<br>
<br>

# Redis 를 이용한 동시성 해결



### 1. Lettuce 사용

레디스 cli 시작

```
docker exec -it myredis redis-cli
```

setnx 를 통한 동시성 test

```
$ setnx 1 lock    -- lock 얻는다
(integer) 1       -- 성공
$ setnx 1 lock    -- lock 얻는다
(integer) 0       -- 실패
```



#### 장점

구현이 간편

spring의 기본 제공 라이브러리

#### 단점

spin Lock 방법이므로  redis 에 무리를 줄수있음(락 획득까지 무한으로 획득 시도를 하기 때문)

재시도 로직을 작성해야함



### 2.redison 사용

lock을 pub,sub 하는 구조

레디스 cli 시작

```
docker exec -it myredis redis-cli
```

채널 구독

```
$ subscribe ch1
```

채널에 메시지(Lock) 발행

```
$ publish ch1 hello
```



#### 장점

pub/sub 기반으로

lettuce 보다 부하가 적음 또한 락 재시도를 대게 제한을 걸기에 부하가 적음

재시도 로직을 기본으로 제공

#### 단점

spring의 기본 제공 라이브러리가 아님, 별도의 라이브러리임









#### 재시도가 필요하지 않은 lock의 경우 Lettuce 사용

```
ex)
유저 1명당 로지텍의 A 상품을 1개밖에 구매할 수 없다고 가정해보겠습니다.

이 상황에서 userid_로지텍A상품을 key 로 lock 을 잡은상태에서 동일한 요청이 들어온다면 락을 기다릴 필요가 없을겁니다.

먼저 들어온 요청이 처리가 된다면 이후의 요청이 lock 을 잡더라도 할 일이 없기때문입니다.

이러한 상황이 재시도가 필요가 없는 상황입니다.
```

#### 재시도가 필요한 lock의 경우 redisson사용



### Mysql vs Redis

- Mysql

  - 별도의 비용없이 사용가능
  - 어느정도 트래픽까지는 문제 없음
  - Redis 보다는 성능이 떨어짐

- Redis

  - 별도의 구축비용과 인프라 관리비용 발생

  - mysql 보다 성능 좋음(Redis는 인메모리 기반)

    

redis 추가 정보

https://goodgid.github.io/Redis/





<br>

<br>

<br>



출처

https://www.inflearn.com/course/lecture?courseSlug=%EB%8F%99%EC%8B%9C%EC%84%B1%EC%9D%B4%EC%8A%88-%EC%9E%AC%EA%B3%A0%EC%8B%9C%EC%8A%A4%ED%85%9C&unitId=114854&category=questionDetail&tab=community&q=682397






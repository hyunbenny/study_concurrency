# study_concurrency


> 실무를 하다보면 동시성과 관련된 문제 부딪히게 되는데 동시성과 관련된 좋은 강의를 찾아 공부하면서 정리하였다.
 
[재고시스템으로 알아보는 동시성 이슈 해결방법](https://www.inflearn.com/course/%EB%8F%99%EC%8B%9C%EC%84%B1%EC%9D%B4%EC%8A%88-%EC%9E%AC%EA%B3%A0%EC%8B%9C%EC%8A%A4%ED%85%9C/dashboard)

동시성 이슈는 아래와 같이 크게 3가지로 나누어서 해결할 수 있다.
1. 자바
2. DB
3. Redis

# 1. 자바
## `synchronized`의 사용
> 동시성 이슈가 발생하면 안되는 메서드의 선언부에 `synchronized`를 붙여 해당 `메서드`에 하나의 쓰레드만 접근 가능하도록 해준다.

```java

@Transactional
public synchronized void decreaseStock(long id, long decreaseQuantity){
	...
}
```
위의 decreaseStock()에 `@Transactional`이 붙어 있기 때문에 동작 흐름을 간단하게 보면 아래와 같다.
```java
startTransaction();
decreaseStock();
endTransaction();
```
문제는 위에서 언급했던 `메서드에 하나의 쓰레드만 접근 가능` 하다는 점이다.

왜?

`@Transactional`의 동작원리를 다시 보면 이해가 가능하다.
decreaseStock()이 끝나고 endTransaction() 즉, `commit`이 되기 전에 다른 쓰레드에서 메서드에 접근이 가능하기 때문에 동시성 이슈는 여전히 존재한다.

➡️ 이는 `@Transactional`을 지워주면 해결된다.

하지만 또 다른 문제점이 존재한다.
`synchronized`는 `자바코드`이다. 

이 말은 한 프로세스 안에서만 동시성 이슈가 보장된다는 말이며 즉, 여러 대의 서버를 사용하는 실무 환경에서는 아무런 소용이 없다는 말이다. 

그러면 이를 해결할 수 있는 DB에서 제공하는 `Lock`에 대해서 알아보자

# 2. DB Lock
> Mysql에서는 아래와 같은 3가지 `Lock`을 제공한다.<br/>

https://dev.mysql.com/doc/refman/8.0/en/ <br/>
https://dev.mysql.com/doc/refman/8.0/en/locking-functions.html <br/>
https://dev.mysql.com/doc/refman/8.0/en/metadata-locking.html <br/>

## 2.1 Pessimistic Lock
> 실제 데이터에 `Lock`을 걸어서 정합성을 맞추는 방법
- exclusive lock 을 걸게되며 다른 트랜잭션에서는 lock 이 해제되기전에 데이터를 가져갈 수 없으므로 데이터 정합성이 보장된다.
- 충돌이 빈번하게 일어날 수 있는 메서드에서는 `Optimistic Lock` 보다 성능이 좋다.
- 주의 : 성능의 감소와 `Deadlock`이 걸릴 수 있다.

```java
@Lock(value = LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Stock  s WHERE s.id = :id")
Stock findByIdWithPessimisticLock(Long id);

// 마이바티스를 사용한다면 쿼리에 `FOR UPDATE`를 붙여주면 된다.
SELECT * FROM stock WHERE id = {#id} FOR UPDATE WAIT 10;
```
## 2.2 Optimistic Lock
> 실제로 `Lock` 을 이용하지 않고 `버전`을 이용함으로써 정합성을 맞추는 방법
- 먼저 데이터를 읽은 후에 update 를 수행할 때 현재 내가 읽은 버전이 맞는지 확인하며 업데이트 합니다.
- 버전이 다른 경우에는 application에서 다시 읽은후에 작업을 수행해야 한다.
- 실제 `Lock`을 잡지 않기 때문에 `Pessimistic Lock`보다 성능상 이점이 있다.
- update로직이 실패했을 때 `retry` 하는 로직을 개발자가 직접 작성해줘야 한다.
```java
// 엔티티에 `version`필드를 추가해줘야 한다.
@Version
private Long version;
```
```java
@Lock(value = LockModeType.OPTIMISTIC)
@Query("SELECT s FROM OptimisticLockStock  s WHERE s.id = :id")
OptimisticLockStock findByIdWithOptimisticLock(Long id);
```
```java
// `Lock`이 걸려 로직에 실패한 경우, 재시도하는 로직을 개발자가 직접 짜줘야 한다.
@Service
@RequiredArgsConstructor
public class OptimisticLockStockServiceFacade {

    private final OptimisticLockStockService optimisticLockStockService;

    public void decrease(long id, long quantity) throws InterruptedException {

        while (true) {
            try {
                optimisticLockStockService.decrease(id, quantity);
                break;
            } catch (Exception e) {
                Thread.sleep(50);
            }
        }
    }

}
```
## 2.3 Named Lock
> 이름을 가진 `metadata locking`
- 이름을 가진 lock 을 획득한 후 해제할때까지 다른 세션은 이 lock 을 획득할 수 없도록 합니다.
- 주로 `분산 락`을 구현할 때 사용한다.
- timout을 구현하기 힘든 `Pessimistic Lock` 과는 달리 쉽게 구현할 수 있다.
- 주의: transaction이 종료될 때 lock 이 자동으로 해제되지 않는다
    - 따라서 별도의 명령어로 `직접 해제` 시켜주거나 `선점시간`이 끝나야 해제된다.
        - mysql : `get_lock`, `release_lock`

```java
public interface NamedLockRepository extends JpaRepository<Stock, Long> {

    @Query(value = "SELECT GET_LOCK(:key, 3000)", nativeQuery = true)
    void getLock(String key);

    @Query(value = "SELECT RELEASE_LOCK(:key)", nativeQuery = true)
    void releaseLock(String key);
}
```
```java
@Service
@RequiredArgsConstructor
public class NamedLockStockService {

    private final StockRepository stockRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized void decrease(Long id, Long quantity) {
        // get stock entity
        Stock stock = stockRepository.findById(id).orElseThrow();

        // decrease stock
        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);
    }
}
```
```java
@Component
@RequiredArgsConstructor
public class NamedLockStockServiceFacade {

    private final NamedLockRepository lockRepository;

    private final NamedLockStockService stockService;

    @Transactional
    public void decrease(Long id, Long quantity) {
        try {
            lockRepository.getLock(String.valueOf(id));
            stockService.decrease(id, quantity);
        } finally{
            lockRepository.releaseLock(String.valueOf(id));
        }

    }

}
```
# 3. Redis
## 3.1 Lettuce
> setnx 명령어를 사용한 `분산 락` 구현으로 `spin lock` 방식을 사용한다.
- [spin lock](https://ko.wikipedia.org/wiki/%EC%8A%A4%ED%95%80%EB%9D%BD) : Lock을 획득하려는 쓰레드가 Lock을 사용할 수 있는지 반복적으로 확인하면서 lock을 획득하는 방식
    - → 락을 획득할 때까지 `retry` 하는 로직을 개발자가 직접 작성해줘야 한다.
- 구현이 간단하다는 장점이 있지만, `Spin Lock`방식이기 때문에  `Redis`에 부하를 줄 수 있다.
```java
@Component
@RequiredArgsConstructor
public class RedisLettuceLockRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public Boolean lock(Long key) {
        return redisTemplate
                .opsForValue()
                .setIfAbsent(generatedKey(key), "lock", Duration.ofMillis(3_000));

    }

    public Boolean unlock(Long key) {
        return redisTemplate.delete(generatedKey(key));
    }

    private String generatedKey(Long key) {
        return String.valueOf(key);
    }
}
```
```java
@Component
@RequiredArgsConstructor
public class LettuceLockStockServiceFacade {

    private final RedisLettuceLockRepository redisRepository;
    private final StockService stockService;

    public void decrease(Long id, Long quantity) throws InterruptedException {
        while (!redisRepository.lock(id)) {
            Thread.sleep(100); // Redis에 많은 부하가 가는 것을 막기 위해서 `Thread.sleep()`을 통해 부하를 좀 줄여주자.
        }

        try {
            stockService.decrease(id, quantity);
        } finally{
            redisRepository.unlock(id);
        }

    }
}
```
## 3.2 Redisson
> `pub-sub` 방식을 기반으로 락을 구현하여 제공한다.
- `채널`을 만들고 락을 점유하고 있던 쓰레드의 작업이 끝나면 채널은 획득하기 위해서 대기 중인 쓰레드에게 락의 해제를 알려주고 그러면 대기 중이던 쓰레드가 락을 획득하는 방식
- `Redisson` 은 `Lock`획득과 관련된 클래스를 제공하기 때문에 개발자가 직접 `리포지토리`를 만들지 않아도 된다.
- `Lettuce`에 비해서 구현이 조금은 복잡하고 별도 라이브러리를 사용해야 한다는 단점이 존재하지만,
- `pub-sub` 방식이기 때문에 `Redis` 에 부하를 줄여준다는 장점이 있다.
```java
@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonLockStockServiceFacade {

    private final RedissonClient redissonClient;
    private final StockService stockService;

    public void decrease(Long id, Long quantity) {
        RLock lock = redissonClient.getLock(String.valueOf(id));

        try {
            boolean available = lock.tryLock(5, 1, TimeUnit.SECONDS);

            if (!available){
                log.info("Lock 획득 실패");
                return;
            }

            stockService.decrease(id, quantity);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            lock.unlock();
        }
    }
}
```


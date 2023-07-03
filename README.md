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
> Mysql에서는 아래와 같은 3가지 `Lock`을 제공한다.

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

> 이 글은 김영한님의 **'스프링 부트와 JPA 실무 완전 정복 로드맵'** 강의를 듣고 정리한 내용입니다.
> 강의 : [실전! 스프링 데이터 JPA](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81-%EB%8D%B0%EC%9D%B4%ED%84%B0-JPA-%EC%8B%A4%EC%A0%84/)

# 도메인 분석
```java
@Getter @ToString(of = {"id", "username", "age"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
    
    public void changeTeam(Team team) {
        this.team = team;
        team.getMembers().add(this);
    }
}
```

```java
@Getter @ToString(of = {"id", "name"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Team {

    @Id @GeneratedValue
    @Column(name = "team_id")
    private Long id;
    private String name;

    @OneToMany(mappedBy = "team")
    private List<Member> members = new ArrayList<>();
}
```

* `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
  * 엔티티는 기본 생성자를 가지고 있어야 한다.
  * `public` 으로 설정하면, 다른 개발자가 기본 생성자를 통해서 엔티티를 생성할 수 있다. 
  * 엔티티 생성에 필요한 값은 전부 생성자를 통해서 입력받는다.
    * `setXXX()` 프로퍼티 접근법은 사용하지 말자.
* `@Column(name = "team_id")`
  * 설정하지 않을 경우, 모든 테이블의 PK 가 `id` 컬럼이 된다.
  * 각 테이블 명을 포함한 `team_id` 로 설정한다.
  * 반드시 해야하는 것은 아니다.
* `@ToString`
  * 연관관계 없는 필드만 설정한다.
  * 연관관계 필드는 무한 루프에 빠질 수 있다.
  
---

# 쿼리 메소드 기능
## 메소드 이름으로 쿼리 생성
```java
@Repository
public class MemberJpaRepository {

    @PersistenceContext
    private EntityManager em;
    
    public List<Member> findByUsernameAndAgeGreaterThan(String username, int age) {
        return em.createQuery("select m from Member m " +
                        "where m.username = :username and m.age > :age", Member.class)
                .setParameter("username", username)
                .setParameter("age", age)
                .getResultList();
    }
}
```
```java
public interface MemberRepository extends JpaRepository<Member, Long> {

	List<Member> findByUsernameAndAgeGreaterThan(String username, int age);
}
```
* `@Repository`
  * 컴포넌트 스캔을 통해 스프링 빈으로 자동 등록된다.
  * JPA 예외를 스프링 데이터 JPA 예외(`RuntimeException`)로 변환한다.
* `public interface MemberRepository extends JpaRepository<Member, Long>`
  * 스프링 데이터 JPA 가 `MemberRepository` 구현체를 생성해준다.
  * 구현체인 `SimpleJpaRepository` 에 `@Repository`, `@Transactional` 애노테이션이 선언되어 있다.
* `List<Member> findByUsernameAndAgeGreaterThan(String username, int age)`
  * 메소드 이름으로 쿼리를 생성한다.
  * 엔티티 필드 명과 메소드 이름이 같아야 한다.
  * 내부적으로 JPA `setParameter` 를 사용한다.
* 파라미터가 증가하면 메소드 이름이 너무 길어진다. → `@Query` 를 사용한다.
  
## JPA NamedQuery
```java
@Entity
@NamedQuery(
        name = "Member.findByUsername",
        query = "select m from Member m where m.username = :username"
)
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;
}
```
```java
public interface MemberRepository extends JpaRepository<Member, Long> {

	List<Member> findByUsername(String username);
}
```

* 엔티티에 `@NamedQuery` 애노테이션을 통해 정적 쿼리를 정의할 수 있다.
  * 여러 메서드에서 사용할 수 있다.
  * 애플리케이션 실행 시점에 문법 오류를 발견할 수 있다.
* 스프링 데이터 JPA 는 자동으로 `@NamedQuery` 를 찾아서 실행한다.
  * "도메인 클래스 + . + 메서드 이름" 방식으로 찾는다.
* 만약 `@NamedQuery` 가 없으면 메소드 이름으로 쿼리 생성 전략을 사용한다.
* `NamedQuery` 를 사용하지 않는다. → 대신에 `@Query` 를 사용한다.
  * `NamedQuery` 는 엔티티가 더러워진다.

## @Query
```java
public interface MemberRepository extends JpaRepository<Member, Long> {

	@Query("select m from Member m where m.username = :username and m.age = :age")
    List<Member> findUser(@Param("username") String username, @Param("age") int age);
}
```
* `jpql` 쿼리를 직접 작성한다.
* `NamedQuery` 처럼 애플리케이션 실행 시점에 문법 오류를 발견할 수 있다.
* 자유롭게 메소드 이름을 설정할 수 있다.
* 정적 쿼리는 메소드 이름 쿼리와 `@Query` 기능으로 종결된다.
* 동적 쿼리는 `QuerDSL` 을 사용한다.

---

# 쿼리 메소드 이해
## 파라미터 바인딩
```sql
select m from Member m where m.username = ?0    // 위치 기반
select m from Member m where m.username = :name // 이름 기반
```
```java
@Query("select m from Member m where m.username in :names")
List<Member> findByNames(@Param("names") List<String> names);
```

* 파라미터 바인딩은 위치 기반, 이름 기반 2가지 방법이 있다.
* 무조건 이름 기반 파라미터 바인딩을 사용하자.
  * 위치 기반은 순서가 바뀔 수 있다.
* 컬렉션 파라미터 바인딩이 가능하다.
  * IN 쿼리가 지원된다.

## 반환 타입
```java
public interface MemberRepository extends JpaRepository<Member, Long> {

	List<Member> findByUsername(String username);
    Member findByUsername(String username);
    Optional<Member> findByUsername(String username);
}
````
* `List<Member>`
  * 결과가 없으면 빈 컬렉션을 반환한다.
* `Member`
  * 결과가 없으면 `null` 을 반환한다.
  * 결과가 2개 이상이면 `NonUniqueResultException` 예외가 발생한다.
* `Optional<Member>`
  * 단건 조회일 경우, `Optional` 을 사용하자.

> **참고**
> 반환 타입만 다르고 메소드 이름, 매개변수가 같으면 오버로딩이 동작하지 않는다. 컴파일 오류가 발생한다.

## 페이징과 정렬
```java
public interface Page<T> extends Slice<T> {
	int getTotalPages();     // 전체 페이지 수
    long getTotalElements(); // 전체 데이터 수
    <U> Page<U> map(Function<? super T, ? extends U> converter); // 변환
}
```
```java
public interface MemberRepository extends JpaRepository<Member, Long> {

	Page<Member> findPageByAge(int age, Pageable pageable);
    Slice<Member> findSliceByAge(int age, Pageable pageable);
    List<Member> findListByAge(int age, Pageable pageable);
    
    @Query(value = "select m from Member m", countQuery = "select count(m.id) from Member m")
    Page<Member> findByAgeDetachCountQuery(int age, Pageable pageable);
}
```
```java
@Test
public void paging() {
    // Given
    memberRepository.save(new Member("member1", 10));
    memberRepository.save(new Member("member2", 10));
    memberRepository.save(new Member("member3", 10));
    memberRepository.save(new Member("member4", 10));
    memberRepository.save(new Member("member5", 10));

    PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));
    int age = 10;

    // When
    Page<Member> page = memberRepository.findByAge(age, pageRequest);

    Page<MemberDto> map = page.map(member -> new MemberDto(
                member.getId(), 
                member.getUsername(),
                member.getTeam().getName()));

    // Then
    List<Member> content = page.getContent();
    int totalPages = page.getTotalPages();        // 전체 페이지 수
    long totalElements = page.getTotalElements(); // 전체 데이터 수
}
```

* `Page<Member>`
  * 추가로 count 쿼리가 자동으로 실행된다.
    * `Page` 인터페이스의 `getTotalPages()` 메소드를 위해 반드시 필요하다.
* `Slice<Member>`
  * 페이지 없이 '더보기'로 데이터를 확인하는 방법이다.
    * 다음 페이지만 확인이 가능하다.
    * count 쿼리가 필요없다.
  * 내부적으로 `limit + 1` 조회한다.
    * 다음 데이터가 있는지 바로 확인 가능하다.
* `List<Member>`
  * 페이지 없이 결과만 반환한다.
  * count 쿼리가 필요없다.
* `@Query(value = "select m from Member m", countQuery = "select count(m) from Member m")`
  * 조인을 사용할 경우, 조인 테이블을 대상으로 count 쿼리가 실행된다. 👉 성능 이슈
  * 별도로 count 쿼리를 정의할 수 있다.
* `PageRequest`
  * `Pageable` 인터페이스 구현체이다.
  * 생성자 파라미터는 (현재 페이지, 조회할 데이터 수, 정렬 정보) 와 같다.
* `page.map()`
  * 엔티티를 반환하면 안 된다.
  * DTO 로 변환해서 반환할 때 사용한다.

> **참고**
> 스프링 데이터 JPA 에서 제공하는 `Page` 는 0부터 시작한다.

## 벌크 수정 쿼리
```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Modifying(clearAutomatically = true) 
    @Query("update Member m set m.age = m.age + 1 where m.age >= :age")
    int bulkAgePlus(@Param("age") int age);
}
```
* 변경 감지(`Dirty Checking`) 없이 대용량 데이터를 수정 및 삭제할 때 사용한다.
  * 변경 감지는 실시간 대응에 알맞는 방법이다.
  * 변경 감지는 N 개 엔티티를 수정하면 N 개 update 쿼리가 실행된다.
* 벌크 수정, 삭제 쿼리는 `@Modifying` 애노테이션을 사용한다.
  * 내부적으로 JPA `executeUpdate()` 벌크 연산을 실시한다.
  * 사용하지 않으면 예외가 발생한다.
* `(clearAutomatically = true)`
  * 벌크 연산 후 자동으로 영속성 컨텍스트를 초기화한다.

> **참고**
> 벌크 연산은 영속성 컨텍스트를 무시하고 데이터베이스에 직접 쿼리를 실행한다. 데이터베이스와 영속성 컨텍스트의 엔티티가 달라질 수 있다.
> 1. 벌크 연산을 제일 먼저 실행한다. 영속성 컨텍스트에 엔티티가 없는 상태이기 때문에 문제없다.
> 2. 벌크 연산 후, 영속성 컨텍스트를 초기화한다.

## @EntityGraph
```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Override
    @EntityGraph(attributePaths = "team")
    List<Member> findAll();
}
```

* member → team 은 지연로딩 관계이다. memberList 를 조회할 때, `1 + N` 문제가 발생한다.
  * member 조회하는 쿼리 1개, 각 member 의 team 을 조회하는 쿼리 N 개
  * 반대로 team 을 조회할 때도 `1 + N` 문제가 발생한다.
    * team 조회하는 쿼리 1개, team 의 각 member 를 조회하는 쿼리 N 개
  * 페치 조인이 필요하다.
* `@EntityGraph`
  * 간단한 페치 조인을 사용할 때, `@EntityGraph` 를 사용한다.
    * 내부적으로 LEFT OUTER JOIN 을 사용한다.
  * 복잡한 페치 조인은 직접 `jpql` 쿼리를 작성한다.

## JPA Hiny & Lock
```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    @QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Member findReadOnlyByUsername(String username);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Member findLockByUsername(String username);
}
```

* `@QueryHints`
  * JPA 구현체에게 힌트를 제공할 수 있다.
  * `org.hibernate.readOnly` 를 사용하면 `em.flush()` 를 해도 update 쿼리가 실행되지 않는다.
* `@Lock(LockModeType.PESSIMISTIC_WRITE)`
  * `select for update` 쿼리가 실행된다. 비관적 락이다.
  * 실시간 트래픽이 많은 곳에서는 사용하면 안 된다. 낙관적 락을 사용하자.

---

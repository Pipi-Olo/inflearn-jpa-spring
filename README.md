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
* `@JoinColumn(name = "team_id")`
  * 조인할 때 필요한 외래키를 설정한다. `Team` 테이블의 PK 컬럼으로 해야한다.

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

	// @Query(name = "Member.findByUsername")
	List<Member> findByUsername(String username);
}
```

* 엔티티에 `@NamedQuery` 애노테이션을 통해 정적 쿼리를 정의할 수 있다.
  * 여러 메소드에서 사용할 수 있다.
  * 애플리케이션 실행 시점에 문법 오류를 발견할 수 있다.
* 스프링 데이터 JPA 는 자동으로 `@NamedQuery` 를 찾아서 실행한다.
  * "도메인 클래스 + . + 메소드 이름" 방식으로 찾는다.
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
    
    @Query(value = "select m from Member m", 
           countQuery = "select count(m) from Member m")
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

    PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, 
                                                           "username"));
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

> **참고**
> JDBC Template, MyBatis 등 다른 기술과 JPA 같이 쓸 때, 영속성 컨텍스트를 주의해야 한다. 벌크 연산 처럼 JPA 가 데이터를 인식하지 못 하기 때문에 초기화(`em.flush()`∙`em.clear()`) 작업이 필요하다.

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

> **참고**
> `1 + N` 문제는 쿼리가 N 개 실행된다는 것도 성능 문제이지만, 애플리케이션에서 데이터베이스까지 N 번의 네트워크를 이용해야 한다는 것도 심각한 성능 저하 원인이다.

## JPA Hint & Lock
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

> **참고**
> 영속성 컨텍스트는 내부에 엔티티 객체를 2개 (스냅샷 1개, 현 상태 엔티티 1개) 가지고 있다.
> `@QueryHint(name = "org.hibernate.readOnly", value = "true"))` 는 내부에 스냅샷을 만들지 않아 변경 감지가 동작하지 않는다. 이를 통해 미미한 성능 향상을 얻을 수 있다.
> 성능 문제가 발생했을 때, `@QueryHint` 를 통해 원하는 성능을 얻을 수 없다. 스프링 데이터 JPA 로 해결 불가능한 성능 문제는 캐시를 사용해야 한다.
> 패키지를 통해 알 수 있듯이 JPA 가 제공하는 기술이 아닌, 하이버네이트 전용 기술이다.

---

# 스프링 데이터 JPA 확장
## 사용자 정의 리포지토리
```java
public interface MemberRepository extends 
        JpaRepository<Member, Long>, 
        MemberRepositoryCustom {
}
```
```java
public interface MemberRepositoryCustom {
    List<Member> findMemberCustom();
}
```
```java
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {

    private final EntityManager em;

    @Override
    public List<Member> findMemberCustom() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }
}
```
```java
@RequiredArgsConstructor
@Repository
public class MemberQueryRepository {

    private final EntityManager em;

    public List<MemberDto> findAll() {
        return em.createQuery("select new datajpa.MemberDto(m.id, m.username) " +
                              "from Member m", MemberDto.class)
                .getResultList();
    }
}

```

* 사용자 정의 리포지토리 클래스 이름은 인터페이스 이름 뒤에 `Impl` 을 붙여야 한다.
  * `MemberRepositoryImpl` 혹은 `MemberRepositoryCustomImpl` 둘다 가능하다.
    * `MemberRepositoryCustomImpl` 을 추천한다.
  * 스프링 데이터 JPA 가 스프링 빈으로 등록해준다.
  * 기존 `memberRepository` 에 기능이 추가된다.
* 임의의 리포지토리를 등록할 수 있다.
  * `MemberQueryRepository` 클래스를 생성해서 `@Repository` 애노테이션을 붙이면 된다.
  * 스프링 데이터 JPA 와 무관하게 동작한다.
    * 서비스 계층에서 `memberQueryRepository` 멤버 변수를 추가적으로 선언해야 한다.

> **참고**
> 스프링 데이터 JPA 는 인터페이스만 정의하면 구현체는 자동으로 생성해준다.
> `QueryDSL`, `MyBatis`, `EntityManager` 등 스프링 데이터 JPA 기술 이외의 다른 기술을 사용할 때, 사용자 정의 리포지토리를 사용한다. `JpaRepository` 인터페이스 구현체를 만드는 것은 너무 많은 기능을 직접 구현해야 한다.

> **참고**
> `MemberQueryRepository` 클래스는 특정 화면이나 API 에 의존적인 Query 를 사용할 때 쓰는 리포지토리이다. 
> 핵심 비지니스 로직과 API 로직의 유지보수 라이프 사이클이 다르기 때문에 별도로 관리해야 한다. 주로 DTO 형태로 조회한다.

## Auditing
```java
@EnableJpaAuditing
@SpringBootApplication
public class DataJpaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataJpaApplication.class, args);
    }
    
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(UUID.randomUUID().toString());
    }
}
```
```java
@Getter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public class BaseTimeEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}
```
```java
@Getter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public class BaseEntity extends BaseTimeEntity {

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;
}
```
```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Member extends BaseEntity {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;
}
```
* `@EnableJpaAuditing`
  * JPA Auditing 기능을 사용하기 위해서는 선언해야 한다.
* 대부분의 엔티티는 등록∙수정 시간이 필요하지만, 등록자∙수정자는 필요 없을 수도 있다.
  * `BaseTimeEntity` 클래스와 `BaseEntity` 클래스로 분리한다.
* `AuditorAware<String> auditorProvider()`
  * 등록자∙수정자를 처리하는 로직이 별도로 필요하다.
  * 일반적으로 세션 정보나 스프링 시큐리티 로그인 정보를 사용한다.

## Web 확장 - 도메인 클래스 컨버터
```java
@RestController
public class MemberController {
    
    @GetMapping("/members2/{id}")
    public String findMember(@PathVariable("id") Member member) {
        return member.getUsername();
    }
}
```

* 매개 변수로 Member 엔티티를 받을 수 있다.
  * 도메인 클래스 컨버터도 내부적으로 `Id` 를 이용해 리포지토리에서 엔티티를 찾는다.
* 사용하지 말자.
  * 트랜잭션이 없는 범위에서 엔티티를 사용하는 것은 위험하다.
  * `OSIV` 참고하자.

## Web 확장 - 페이징과 정렬
```java
@RestController
public class MemberController {

    @GetMapping("/members")
    public Page<MemberDto> members(
            @PageableDefault(size = 5, sort = "username") Pageable pageable
    ) {
        return memberRepository.findAll(pageable)
                .map(MemberDto::new);
    }
}
```
```yml
spring:
  data:
    web:
      pageable:
        default-page-size: 20 # 기본 페이지 사이즈
        max-page-size: 2000   # 최대 페이지 사이즈
        one-indexed-parameters: true
```

* `Pageable` 를 파라미터로 받을 수 있다.
  * `Pageable` 은 인터페이스이다. 구현체로 `PageRequest` 가 넘어온다.
* `/members?page=0&size=3&sort=id,desc` 요청 파라미터 형식으로 넘어온 URL 을 통해 `PageRequest` 객체를 생성한다.
  * `page` 는 0 부터 시작한다.
  * `size` 👉 한 페이지에 노출할 데이터 수
  * `sort` 👉 정렬 조건을 정의한다. `&` 로 여러 정렬 조건을 받을 수 있다.
* 엔티티를 외부에 노출하면 안 된다.
  * `page.map()` 을 통해 `MemberDto` 로 변환한다.
* `@PageableDefault`
  * 개별적으로 기본 값을 설정할 수 있다.
  * 글로벌 설정은 `application.yml` 을 사용한다.
* `one-indexed-parameters: true`
  * HTTP 요청 `page` 가 1 부터 시작한다.
  * 하지만 HTTP 응답에는 여전히 페이지가 0 부터 시작한다.
  * 사용하지 말자.
  
---

# 스프링 데이터 JPA 분석
## 스프링 데이터 JPA 구현체
```java
@Repository
@Transactional(readOnly = true)
public class SimpleJpaRepository<T, ID> implements JpaRepositoryImplementation<T, ID> {

	private final EntityManager em;
	
    ...

	@Transactional
	@Override
	public <S extends T> S save(S entity) {

		Assert.notNull(entity, "Entity must not be null.");

		if (entityInformation.isNew(entity)) {
			em.persist(entity);
			return entity;
		} else {
			return em.merge(entity);
		}
	}
    
    ...
}
```
* `@Repository`
  * 컴포넌트 스캔을 통해 스프링 빈으로 등록한다.
  * JPA 예외를 스프링 데이터 예외로 변환한다.
* `@Transactional`
  * JPA 의 모든 변경은 트랜잭션 안에서 동작한다.
  * 서비스 계층에서 트랜잭션을 시작하지 않아도 리포지토리 계층은 트랜잭션이 동작한다.
* `@Transactional(readOnly = true)`
  * 트랜잭션을 커밋해도 JPA 플러쉬가 발생하지 않는다. → 성능 향상
* `save()`
  * 새로운 엔티티면 저장한다. 👉 `em.persist()`
  * 새로운 엔티티가 아니면 병합한다. 👉 `em.merge()`
  
## JPA 가 새로운 엔티티 구별하는 방법
```java
@Transactional
@Override
public <S extends T> S save(S entity) {

	if (entityInformation.isNew(entity)) {
		em.persist(entity);
		return entity;
	} else {
		return em.merge(entity);
	}
}
```
```java
@Entity
public class Member extends BaseTimeEntity implements Persistable<String> {

    @Id
    private String id;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return createdDate == null;
    }
}
```

* 새로운 엔티티 판단 전략
  * 식별자가 객체일 때, `null` 여부로 판단한다.
  * 식별자가 기본 타입일 때, `0` 여부로 판단한다.
  * `Persistable` 인터페이스를 구현한 판단 로직으로 구별한다.
* `Persistable.isNew()`
  * Member 엔티티는 직접 할당을 통해 JPA 식별자를 생성하고 있다.
  * 오버라이딩을 통해 새로운 엔티티 판단 로직을 작성한다.
  * 생성일 데이터를 통해 새로운 엔티티인지 판단한다.
    * 대부분의 엔티티는 생성일∙수정일 컬럼은 반드시 필요하다.
  
  
> **참고**
> JPA 식별자 생성 전략이 `@GeneratedValue` 이면, `save()` 호출 시점에 식별자가 없으므로 새로운 엔티티로 인식한다. **직접 할당을 통해 식별자를 생성한다면,** 식별자에 값이 들어간 상태에서 `save()` 를 호출하게 된다. 이 떄, `em.merge()` 가 호출되므로 성능 저하가 발생한다.

> **참고**
> `em.merge()` 는 이미 데이터베이스에 데이터가 있을 때, 새로운 데이터로 덮어쓰기 할 때 사용한다. 데이터베이스에서 select 쿼리를 통해 엔티티를 조회하는 불필요한 쿼리가 나가게 된다. 데이터베이스에 데이터가 없으면 새로운 엔티티로 인식하므로 매우 비효율적이다. 개발자가 직접 사용할 일은 거의 없다.

---

# 나머지 기능
## Specifications

* 내부적으로 `JPA Criteria` 를 사용한다.
  * `Criteria` 는 복잡해서 코드를 읽기가 어렵다.
* `QueryDSL` 을 사용하자.

## Query by Example

* LEFT JOIN 이 불가능하다.
* `QueryDSL` 을 사용하자.

## Projections
```java
public interface UsernameOnly {
	String getUsername();
}
```
```java
public interface UserTeamDto {
	
    String getUsername();
    TeamDto getTeam();
    
    interface TeamDto {
    	String getName();
    }
}
```
```java
public interface MemberRepository extends JpaRepository<Member, Long> {

	List<UsernameOnly> findByUsername(String username);
    <T> List<T> findByUsername(String username, Class<T> type);
}
```

* DTO 조회할 때 사용한다. 필요한 컬럼만 SELECT 한다.
* 프로퍼티 형식의 인터페이스를 작성하면, 스프링 데이터 JPA 가 구현체를 제공한다.
  * 클래스 기반으로도 가능하다. 생성자 파라미터 이름으로 매핑한다.
* 제네릭 타입으로 동적으로 데이터 변경 가능하다.
* UserTeamDto 처럼 조인을 해야할 경우 JPQL SELECT 절 최적화가 불가능하다.
  * 모든 필드를 SELECT 한 뒤, Dto 에 매핑한다.
* 단순한 경우에만 사용하자. 복잡한 경우에는 `QueryDSL` 을 사용하자.

## 네이티브 쿼리
```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query(value = "select * from member where username = ?", nativeQuery = true)
    Member findByNativeQuery(String username);
}
```

* SQL 을 직접 입력하는 방법이다.
* 동적 쿼리가 불가능하다.
* Sort 파라미터를 통한 정렬이 정상 동작하지 않는다.
* JPQL 처럼 애플리케이션 로딩 시점에 문법 오류 확인이 불가능하다.
* 가급접 네이티브 쿼리는 사용하지 말자. → 대신에 스프링 데이터 `Projections` 를 사용하자.
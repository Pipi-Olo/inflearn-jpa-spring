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
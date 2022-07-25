package study.datajpa.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 간단한 쿼리가 필요할 때 사용한다.
    // 메소드 명이 너무 길어져 인식하기 어려워진다.
    // 최대 조건 2개 정도 생각한다.
    List<Member> findByUsernameAndAgeGreaterThan(String username, int age);

    List<Member> findTop3By();

    // @Query(name = "Member.findByUsername")
    // findByUsername -> Member.findByUsername @NamedQuery 를 찾는다.
    // 없으면 메소드 명으로 쿼리를 생성한다.
    // 실무에서는 @NamedQuery 거의 사용 안 한다. -> 리포지토리에 쿼리를 바로 작성하는 Spring Data JPA 기능이 대부분 사용된다.
    // 강력한 장점 : 애플리케이션 로딩 시점에 쿼리 오류를 잡는다.
    // 정적 쿼리 이기 때문에, 미리
    // 기본적으로 jpql 은 문자열이기 때문에, 실제 실행해보기 전까지는 제대로 동작하는지 모른다. 하지만, @NamedQuery 는 다르다.
    List<Member> findByUsername(@Param("username") String username);

    // @NamedQuery 와 동일하게 애플리케이션 로딩 시점에 쿼리 오류를 확인할 수 있다.
    // @NamedQuery 의 강력항 장점과 엔티티를 깔끔하게 유지할 수 있는 매우 좋은 기능이다.
    // 동시에 메소드명으로 쿼리를 만들어주는 기능과 다르게 자유롭게 메소드 명을 설정할 수 있다.
    // 정적 쿼리는 메소드명 쿼리 혹은 @Query 를 사용하는 것이 좋다.
    // 동적 쿼리는 querydsl 사용한다.
    @Query("select m from Member m where m.username = :username and m.age = :age")
    List<Member> findUser(@Param("username") String username, @Param("age") int age);

    @Query("select m.username from Member m")
    List<String> findUsernameList();

    @Query("select new study.datajpa.dto.MemberDto(m.id, m.username, t.name) from Member m join m.team t")
    List<MemberDto> findMemberDto();

    @Query("select m from Member m where m.username in :names")
    List<Member> findByNames(@Param("names") Collection<String> names);

    List<Member> findListByUsername(String username);
    Member findMemberByUsername(String username);
    Optional<Member> findOptionalByUsername(String username);

    Page<Member> findByAge(int age, Pageable pageable);
    Slice<Member> findSliceByAge(int age, Pageable pageable);
    List<Member> findListByAge(int age, Pageable pageable); // content 만 받아올 수도 있다.

    @Query(value = "select m from Member m", countQuery = "select count(m.username) from Member m")
    Page<Member> findByAgeDetachCountQuery(int age, Pageable pageable);

    // @Modifying JPA executeUpdate() 벌크 연산을 실시함. 안하면 Exception
    @Modifying(clearAutomatically = true) // true 값을 주면 em.clear() 를 자동으로 실시해 준다. 벌크 연산 - 영속성 컨텍스트 문제를 해결해준다.
    @Query("update Member m set m.age = m.age + 1 where m.age >= :age")
    int bulkAgePlus(@Param("age") int age);

    @Query("select m from Member m left join fetch m.team")
    List<Member> findMemberFetchJoin();

    @Override
//    @EntityGraph(attributePaths = "team") // 페치 조인을 해야된다. -> jpql 을 짜야하는데 귀찮다. -> @EntityGraph 사용
    @EntityGraph("Member.all") // JPA 의 @NamedEntityGraph 기능을 사용 한것
    List<Member> findAll();

    // @EntityGraph 는 @Query 하고도 같이 쓸 수 있고, 메소드 이름 기반 쿼리에도 사용할 수 있다.
    // 참고로 페치 조인은 기본적으로 레프트 아웃터 조인이 나간다.
    // 근데 사실 @EntityGraph 는 JPA 의 @NamedEntityGraph 를 사용한 기능이다.
    // 간단한 경우 SpringDataJPA @EntityGraph 를 쓰면 되고, 복잡한 경우에는 직접 jpql 을 짠다. 혹은 querydsl
}

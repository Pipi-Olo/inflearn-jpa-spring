package study.datajpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;

import java.util.List;

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
}

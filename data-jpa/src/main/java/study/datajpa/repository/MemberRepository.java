package study.datajpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import study.datajpa.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

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
}

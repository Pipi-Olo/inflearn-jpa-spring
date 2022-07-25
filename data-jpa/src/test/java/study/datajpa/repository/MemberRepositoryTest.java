package study.datajpa.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;
import study.datajpa.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional @Rollback(value = false)
@SpringBootTest
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    TeamRepository teamRepository;

    @PersistenceContext
    EntityManager em;

    @Test
    public void testMember() {
        Member member = new Member("userA");
        Member savedMember = memberRepository.save(member);
        Member findMember = memberRepository.findById(savedMember.getId()).get();

        assertThat(findMember.getId()).isEqualTo(member.getId());
        assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        assertThat(findMember).isEqualTo(member);
    }

    @Test
    public void basicCRUD() {
        Member member1 = new Member("member1");
        Member member2 = new Member("member2");

        memberRepository.save(member1);
        memberRepository.save(member2);

        // 단건 조회
        Member findMember1 = memberRepository.findById(member1.getId()).get();
        Member findMember2 = memberRepository.findById(member2.getId()).get();

        assertThat(findMember1).isEqualTo(member1);
        assertThat(findMember2).isEqualTo(member2);

        // 리스트 조회
        List<Member> members = memberRepository.findAll();
        assertThat(members.size()).isEqualTo(2);

        // 카운트
        long count = memberRepository.count();
        assertThat(count).isEqualTo(2);

        // 삭제
        memberRepository.delete(member1);
        memberRepository.delete(member2);

        long deletedCount = memberRepository.count();
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    public void findByUsernameAndAgeGreaterThen() {
        Member member1 = new Member("member", 10);
        Member member2 = new Member("member", 20);

        memberRepository.save(member1);
        memberRepository.save(member2);

        List<Member> members = memberRepository.findByUsernameAndAgeGreaterThan("member", 15);

        assertThat(members.get(0).getUsername()).isEqualTo("member");
        assertThat(members.get(0).getAge()).isEqualTo(20);
        assertThat(members.size()).isEqualTo(1);
    }

    @Test
    public void testNamedQuery() {
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("BBB", 20);

        memberRepository.save(member1);
        memberRepository.save(member2);

        List<Member> members = memberRepository.findByUsername("AAA");
        for (Member member : members) {
            assertThat(member.getUsername()).isEqualTo("AAA");
        }
    }

    @Test
    public void testQuery() {
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("BBB", 20);

        memberRepository.save(member1);
        memberRepository.save(member2);

        List<Member> members = memberRepository.findUser("AAA", 10);

        assertThat(members.size()).isEqualTo(1);
        assertThat(members.get(0)).isEqualTo(member1);
    }

    @Test
    public void findUsernameList() {
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("BBB", 20);

        memberRepository.save(member1);
        memberRepository.save(member2);

        List<String> usernameList = memberRepository.findUsernameList();
        for (String s : usernameList) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void findMemberDto() {
        Team team = new Team("teamA");
        teamRepository.save(team);

        Member member = new Member("AAA", 10, team);
        memberRepository.save(member);

        List<MemberDto> memberDto = memberRepository.findMemberDto();
        for (MemberDto dto : memberDto) {
            System.out.println("dto = " + dto);
        }
    }

    @Test
    public void findByNames() {
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("BBB", 20);

        memberRepository.save(member1);
        memberRepository.save(member2);

        List<Member> members = memberRepository.findByNames(Arrays.asList("AAA", "BBB"));
        for (Member member : members) {
            System.out.println("member = " + member);
        }
    }

    @Test
    public void returnType() {
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("BBB", 20);

        memberRepository.save(member1);
        memberRepository.save(member2);

        List<Member> members = memberRepository.findListByUsername("AAA"); // 단건이라는 보장이 있으면 List 로 안 받아도 된다. 알아서 반환 값에 맞게 SpringDataJPA 가 반환해준다.
        Member findMember = memberRepository.findMemberByUsername("AAA");
        Optional<Member> findOptional = memberRepository.findOptionalByUsername("AAA");

        // 컬렉션 반환 일 때는 알맞은 데이터가 없을 때, 빈 컬렉션을 반환해준다. 절대 NULL 이 아니다.
        // 단건 조회일 때는 알맞은 데이터가 없을 때, NULL 을 반환한다.
        // JPA 는 결과가 없으면 NoResultException 을 터트린다. SpringDataJPA 는 try~catch 로 잡아서 null 을 반환한다.
        // 단건 조회는 Optional 로 받는다.
        // 반환 값이 단건인데, 결과 값이 2개 이상이면 Exception 이 터진다.
    }

    @Test
    public void paging() {
        // Given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 10));
        memberRepository.save(new Member("member3", 10));
        memberRepository.save(new Member("member4", 10));
        memberRepository.save(new Member("member5", 10));
        memberRepository.save(new Member("member6", 10));
        memberRepository.save(new Member("member7", 10));

        // Spring Data JPA 는 페이지가 0부터 시작한다. 주의!
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));
        int age = 10;

        // When
        Page<Member> page = memberRepository.findByAge(age, pageRequest); // Page 안에 totalCount 쿼리가 자동으로 날라간다.
        // 문제가 있다. 만약 조인을 해서 할 경우, 기본 값이 countQuery 도 조인해서 count 값을 가져온다.
        // 하지만, count 쿼리는 (어차피 레프트 조인이라면) 조인할 이유가 없다. -> 성능 이슈 데이터가 많아지면 굉장히 큰 장애
        // 이럴 경우 countQuery를 별도로 구별할 수 있다.
        // Sorting 조건이 복잡해지면 @Query 통해 직접 jpql 입력한다.
        // 단순 최단 3개 조회라면 Top3 조회가 있다.

        // HTTP API 인 경우 DTO 변환해서 반환해야 한다.
        // page.map() 을 통해 편하게 변환이 가능하다.
        Page<MemberDto> map = page.map(member -> new MemberDto(member.getId(), member.getUsername(), member.getTeam().getName()));

        // Then
        List<Member> content = page.getContent();
        long totalElements = page.getTotalElements(); // totalCount

        assertThat(content.size()).isEqualTo(3);
        assertThat(page.getTotalElements()).isEqualTo(7);
        assertThat(page.getNumber()).isEqualTo(0); // page 개수
        assertThat(page.getTotalPages()).isEqualTo(3); // 총 페이지 개수 3개 ==> 7개, 1개 페이지당 3개
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();

        for (Member member : content) {
            System.out.println("member = " + member);
        }
        System.out.println("totalElements = " + totalElements);
    }

    @Test
    public void pagingSlice() {
        // Given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 10));
        memberRepository.save(new Member("member3", 10));
        memberRepository.save(new Member("member4", 10));
        memberRepository.save(new Member("member5", 10));
        memberRepository.save(new Member("member6", 10));
        memberRepository.save(new Member("member7", 10));

        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));
        int age = 10;

        // When
        // Slice 는 총 totalCount 에 관심없다. -> count 쿼리 안 날라간다.
        // limit + 1 개를 가져온 다음에,
        // 더보기 등 으로 구현하는 것
        Slice<Member> page = memberRepository.findSliceByAge(age, pageRequest); // Page 안에 totalCount 쿼리가 자동으로 날라간다.

        // Then
        List<Member> content = page.getContent();

        assertThat(content.size()).isEqualTo(3);
        assertThat(page.getNumber()).isEqualTo(0);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    public void bulkUpdate() {
        // Given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 20));
        memberRepository.save(new Member("member3", 30));
        memberRepository.save(new Member("member4", 40));
        memberRepository.save(new Member("member5", 50));

        // When
        int resultCount = memberRepository.bulkAgePlus(20);

        // 사실 jpql 나가기 전에, em.flush() 가 반영된다.

        // 같은 트랜잭션이면 같은 엔티티 매니저를 사용한다.
        // 리포지토리에서는 영속성 컨텍스트를 초기화 할 수 없다. 엔티티 매니저가 필요하다.
        em.flush(); // 변경사항 반영
        em.clear(); // 영속성 컨텍스트 클리어

        // Then
        assertThat(resultCount).isEqualTo(4);

        // JPA 벌크 연산은 조심해야한다!
        // JPA 벌크 연산은 영속성 컨텍스트없이 동작한다.
        // 분명 벌크 연산을 통해서 20살 이상의 나이를 + 1살 했지만,
        // 영속성 컨텍스트에 있는 멤버는 아직 + 1 살 되지 않았다.
        // 따라서 영속성 컨텍스트의 초기화 작업이 필요하다.
        // 혹은 벌크 연산을 트랜잭션 시작 후 제알 먼저 연산한다.
        // 애초에 영속성 컨텍스트가 비어져 있으므로 영향을 받을 엔티티가 없다.

        // JDBC, MyBatis 랑 JPA 랑 섞어서 쓸 때, 주의해야한다.
        // 벌크연산처럼 JPA 가 인식하지 못 하기 때문에, 플러쉬 - 클리어 작업이 필요하다.
    }
}
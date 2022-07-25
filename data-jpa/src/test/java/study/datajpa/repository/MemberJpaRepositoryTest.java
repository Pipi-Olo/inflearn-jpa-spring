package study.datajpa.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.datajpa.entity.Member;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Transactional @Rollback(value = false)
@SpringBootTest
class MemberJpaRepositoryTest {

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void testMember() {
        Member member = new Member("userA");
        Member savedMember = memberJpaRepository.save(member);
        Member findMember = memberJpaRepository.find(savedMember.getId());

        assertThat(findMember.getId()).isEqualTo(member.getId());
        assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        assertThat(findMember).isEqualTo(member); // equalsTo 메소드 동작. 오버라이드 안 했기 때문에 기본인 == 동작
    }

    @Test
    public void basicCRUD() {
        Member member1 = new Member("member1");
        Member member2 = new Member("member2");

        memberJpaRepository.save(member1);
        memberJpaRepository.save(member2);

        // 단건 조회
        Member findMember1 = memberJpaRepository.findById(member1.getId()).get();
        Member findMember2 = memberJpaRepository.findById(member2.getId()).get();

        assertThat(findMember1).isEqualTo(member1);
        assertThat(findMember2).isEqualTo(member2);

        // 리스트 조회
        List<Member> members = memberJpaRepository.findAll();
        assertThat(members.size()).isEqualTo(2);

        // 카운트
        long count = memberJpaRepository.count();
        assertThat(count).isEqualTo(2);

        // 삭제
        memberJpaRepository.delete(member1);
        memberJpaRepository.delete(member2);

        long deletedCount = memberJpaRepository.count();
        assertThat(deletedCount).isEqualTo(0);
    }

    @Test
    public void findByUsernameAndAgeGreaterThen() {
        Member member1 = new Member("member", 10);
        Member member2 = new Member("member", 20);

        memberJpaRepository.save(member1);
        memberJpaRepository.save(member2);

        List<Member> members = memberJpaRepository.findByUsernameAndAgeGreaterThan("member", 15);

        assertThat(members.get(0).getUsername()).isEqualTo("member");
        assertThat(members.get(0).getAge()).isEqualTo(20);
        assertThat(members.size()).isEqualTo(1);
    }

    @Test
    public void testNamedQuery() {
        Member member1 = new Member("AAA", 10);
        Member member2 = new Member("BBB", 20);

        memberJpaRepository.save(member1);
        memberJpaRepository.save(member2);

        List<Member> members = memberJpaRepository.findByUsername("AAA");
        for (Member member : members) {
            assertThat(member.getUsername()).isEqualTo("AAA");
        }
    }

    @Test
    public void paging() {
        // Given
        memberJpaRepository.save(new Member("member1", 10));
        memberJpaRepository.save(new Member("member2", 10));
        memberJpaRepository.save(new Member("member3", 10));
        memberJpaRepository.save(new Member("member4", 10));
        memberJpaRepository.save(new Member("member5", 10));
        memberJpaRepository.save(new Member("member6", 10));
        memberJpaRepository.save(new Member("member7", 10));

        // page 1 offset = 0, limit = 10
        // page 2 offset = 10, limit = 20

        int age = 10;
        int offset = 0;
        int limit = 3;

        // When
        List<Member> members = memberJpaRepository.findByPage(age, offset, limit);
        long totalCount = memberJpaRepository.totalCount(age);

        // 페이지 계산 공식
        // totalPage = totalCount / size
        // 마지막 페이지.. 시작 페이지.. 등
        // 하지만, 걱정하지 마라. SpringDataJPA 가 다 제공해준다.

        // Then
        assertThat(members.size()).isEqualTo(3); // limit 이 3이기 때문에 0,1,2 가 나오는게 맞다.
        assertThat(totalCount).isEqualTo(7);

        // JPA 는 방언이 있기 때문에 데이터베이스가 변경되어도 페이징 동작한다.
    }
}
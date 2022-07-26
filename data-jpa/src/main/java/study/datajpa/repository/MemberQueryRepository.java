package study.datajpa.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import study.datajpa.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class MemberQueryRepository {

    private final EntityManager em;

    public List<Member> findAllMembers() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    // 특정 화면 혹은 API 에 종속적인 / 의존적인 Query 전용 리포지토리
    // 핵심 비지니스 로직과 유지보수 라이프 사이클이 다르다.
}

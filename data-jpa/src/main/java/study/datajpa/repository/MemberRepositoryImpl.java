package study.datajpa.repository;

import lombok.RequiredArgsConstructor;
import study.datajpa.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;

@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    // @PersistenceContext 생성자로 구현체 받을 수도 있음.
    private final EntityManager em;

    @Override
    public List<Member> findMemberCustom() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    // 항상 사용자 정의 리포지토리가 필요한 것은 아니다.
    // 예를 들어서, 특정 화면이나 API 에 종속적인 복잡한 쿼리를 위한
    // MemberQueryRepository 클래스를 생성해서 @Repository 붙여서 만들면 된다.
    // EntityManager
}

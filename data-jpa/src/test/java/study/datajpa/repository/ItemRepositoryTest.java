package study.datajpa.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import study.datajpa.entity.Item;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ItemRepositoryTest {

    @Autowired
    ItemRepository itemRepository;

    @Test
    public void save() {
        itemRepository.save(new Item("AA")); // 지금 id 값이 설정이 안 되어 있다. 즉 null 로 되어 있다.

        // @Transactional 을 달면 setAutoCommit(false) 를 해주고 commit / rollback 이 전부임
        // readOnly = true 을 기능을 달면 flush() 를 호출하지 않음 -> 디비에 데이터 반여 안 됨, 더티 체킹 x , 약간의 성능 향상
        // 어떤 데이터베이스 드라이버는 좀더 성능 향상을 위한 추가적인 조치가 있다고함. 어떤 건지는 솔직히 모르겠음 확인해보아ㅑ함

        // em.merge()
        // 디비에서 들어온 데이터를 가져온다.
        // 새롭게 들어온 데이터로 덮어쓰기 한다.
        // 트랜잭션 끝나면 플러쉬 발생 -> 더티 체킹으로
        // 단점 : 디비에 추가 쿼리 1개 날라간다. (조회 1번 + 더티 체킹 1번)
        // 머지는 쓰지 않는 것이 좋다.
        // 영속 상태에 벗어난 엔티티를 다시 영속 상태에 넣을 떄 사용하는게 머지이다.

        // 식별자 (@Id) 가 null 이거나 혹은 0이면 새로 넣는 엔티티라 판단한다.
        // Long -> null / long -> 0
        // em.persist() 가 호출되면 그 떄, id 에 값이 들어온다.

        // 문제는! @Id 값을 @GeneratedValue 을 통해 데이터베이스에 위임하는 것이 아니라,
        // 내가 Id 값을 넣어주는 형태라면 어떻게 될 까?
        // em.merge() 가 동작하게 된다!!!
        // select 쿼리가 나간다. 왜? em.merge() 이기 떄문에 데이터베이스에 있을 꺼라 생각하니까!!
        // 그리고 없으니까 다시 insert 쿼리가 날라간다. -> 성능 누수!!!

        // 엔티티가 Persistable를 구현해서 isNew 에 대한 로직을 넣어줘야 한

        // em.merge() 정말 특수한 경우에만 쓰고 기본적으로 쓰지 않겠다라고 생각해야 한다.
    }
}
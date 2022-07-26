package study.datajpa.dto;

import org.springframework.beans.factory.annotation.Value;

public interface UsernameOnly {

    // username과 age를 가져와서 getUsername() 에 넣어준다. -> 오픈 프로젝션 다만 기존 기능은 username 만 조회 쿼리가 나간다면, 이 기느을 사용하게 되면 전체 엔티티를 조회해서 필요한 부분만 가져온다.
    // 오픈 프로젝션 : 전체 엔티티를 가져와서 하는 것
    // 클로즈 : 필요한 부분만 가져오는 것
    @Value("#{target.username + ' ' + target.age}")
    String getUsername(); // 프로퍼티 방식으로 메소드 명 만들어야 함.
}

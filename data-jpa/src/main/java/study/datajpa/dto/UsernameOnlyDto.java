package study.datajpa.dto;


public class UsernameOnlyDto {
    private final String username;

    public UsernameOnlyDto(String username) { // 생성자가 중요하다. 이름으로 매핑한다. 파라미터 명이 같아야함
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}

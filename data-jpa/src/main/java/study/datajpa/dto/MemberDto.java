package study.datajpa.dto;

import lombok.Data;
import study.datajpa.entity.Member;

@Data
public class MemberDto {

    private Long id;
    private String username;
    private String teamName;

    public MemberDto(Long id, String username, String teamName) {
        this.id = id;
        this.username = username;
        this.teamName = teamName;
    }

    // DTO 는 엔티티에 의존해도 된다. 엔티티는 DTO 에 의존하면 안 된다.
    public MemberDto(Member member) {
        this.id = member.getId();
        this.username = member.getUsername();
    }
}

package study.datajpa.entity;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@Getter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public class BaseTimeEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
}

// 생성일과 수정일은 반드시 필요한 경우가 많지만, 생성한 사람과 수정한 사람은 테이블마다 필요한 경우가 있고 아닌 경우가 있다.
// 따라서 BaseTimeEntity 를 최상위 계층으로 두고
// 시간만 필요하면 BaseTimeEntity 를
// 사람도 필요하면 Entity 를 상속한다.

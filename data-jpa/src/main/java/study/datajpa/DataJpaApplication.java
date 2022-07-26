package study.datajpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

@EnableJpaAuditing
@SpringBootApplication
public class DataJpaApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataJpaApplication.class, args);
    }


    // BaseEntity 의 createdBy lastModifiedBy 가 실행될 때 해당 빈 동작한다.
    // 실제로는 세션에서 꺼내서 쓴다.
    // 가끔 createdBy 는 NULL 데이터를 넣는 경우가 있는데 추천하지 않는다.
    // 대부분의 경우 데이터가 널이면 고통스럽다.
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(UUID.randomUUID().toString());
    }
}

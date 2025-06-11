package study.data_jpa.repository;

import org.springframework.beans.factory.annotation.Value;

public interface UsernameOnly {

//    @Value("#{target.username + ' ' + target.age}")//오픈 프로젝션
    String getUsername();
}

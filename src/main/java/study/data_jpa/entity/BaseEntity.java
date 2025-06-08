package study.data_jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public class BaseEntity {
    //생성일
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    //수정일
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    //생성자
    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    //수정자
    @LastModifiedBy
    private String lastModifiedBy;
}

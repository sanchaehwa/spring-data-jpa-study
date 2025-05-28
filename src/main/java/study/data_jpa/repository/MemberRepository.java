package study.data_jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import study.data_jpa.entity.Member;
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
}

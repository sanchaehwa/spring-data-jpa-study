package study.data_jpa.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.data_jpa.entity.Member;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Transactional //Transactional + SpringBootTest = 테스트가 끝나면 롤백(DB에는 아무일도 안일어남)
@Rollback(false) // 테스트에서 수행한 DB 변경 사항이 커밋되어(성공하면) 실제 DB에 반영되도록 설정 (디버깅이나 확인 목적)로
class MemberRepositoryTest {
   // 생성자주입
    @Autowired
    MemberRepository memberRepository;
    @Test
    public void testMember() {
        Member member = new Member("memberA");
        Member saveMember = memberRepository.save(member);
        //Optional: 값이 있을수도 있고, 없을 수도 있는 상황을 명시적으로 표현하기 위한 컨테이너 객체
        Member member1 = memberRepository.findById(saveMember.getId())
                        .orElseThrow(() -> new IllegalStateException("회원을 찾을수 없습니다"));

        assertThat(member1.getId()).isEqualTo(member.getId()); //ID 비교
        assertThat(member1.getUsername()).isEqualTo(member.getUsername()); //name 비교
        assertThat(member1).isEqualTo(member); //객체 비교

    }

}

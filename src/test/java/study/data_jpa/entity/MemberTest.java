package study.data_jpa.entity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.data_jpa.repository.MemberRepository;

import java.util.List;

@SpringBootTest
@Rollback(false) // 테스트에서 수행한 DB 변경 사항이 커밋되어(성공하면) 실제 DB에 반영되도록 설정 (디버깅이나 확인 목적)로
@Transactional
class MemberTest {

    @PersistenceContext
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @Test
    public void testEntity() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamB);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamA);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
        
        //초기화
        em.flush();
        em.clear();
        
        //확인
        List<Member> member = em.createQuery("select m from Member m", Member.class).getResultList();
        
        for (Member m : member) {
            System.out.println(m);
            System.out.println("m.getTeam() = " + m.getTeam());
        }

    }
    @Test
    public void JpaEvenetBaseEntity() throws Exception {
        //given
        Member member1 = new Member("member1");
        memberRepository.save(member1);

        Thread.sleep(100);
        member1.setUsername("member2");

        em.flush();
        em.clear();
        //when
        Member findMember = memberRepository.findById(member1.getId()).get();

        //then
        System.out.println("findMember.getCreatedDate = " + findMember.getCreatedDate());
        System.out.println("findMember.getUpdatedDate() = " + findMember.getLastModifiedDate());
        System.out.println("findMember.getTeam() = " + findMember.getCreatedBy());
        System.out.println("findMember.getLasModifiedBy = " + findMember.getLastModifiedBy());

    }
}

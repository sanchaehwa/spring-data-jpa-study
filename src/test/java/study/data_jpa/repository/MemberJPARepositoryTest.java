package study.data_jpa.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.data_jpa.entity.Member;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
@SpringBootTest
@Transactional
@Rollback(false)
class MemberJPARepositoryTest {

    @Autowired
    MemberJPARepository memberJPARepository;
    @Autowired
    private MemberRepository memberRepository;

    @Test
    public void testMember() {
        Member member = new Member("memberA");
        Member saveMember = memberRepository.save(member);

        Member findMember = memberRepository.findById(saveMember.getId()).get();

        assertThat(findMember.getId()).isEqualTo(member.getId());
        assertThat(findMember.getUsername()).isEqualTo(member.getUsername());
        assertThat(findMember).isEqualTo(member);
    }

    @Test
    public void basicCRUD() {
        List<Member> members = new ArrayList<>();
        Member member1 = new Member("member1");
        Member member2 = new Member("member2");
        memberJPARepository.save(member1);
        members.add(member1);
        memberJPARepository.save(member2);
        members.add(member2);

        // 단건 조회
        Member findMember1 = memberJPARepository.findById(member1.getId()).get();
        Member findMember2 = memberJPARepository.findById(member2.getId()).get();
        assertThat(findMember1).isEqualTo(member1);
        assertThat(findMember2).isEqualTo(member2);

        // 리스트 조회 검증
        List<Member> all = memberJPARepository.findAll();
        assertThat(all.size()).isEqualTo(members.size());

        long count = memberJPARepository.count();
        assertThat(count).isEqualTo(members.size());

        // 삭제 검증
        memberJPARepository.delete(member1);
        memberJPARepository.delete(member2);
        long deletedCount = memberJPARepository.count();
        assertThat(deletedCount).isEqualTo(0);
    }
    @Test
    public void findByUsernameAndAgeGreaterThan() {
        Member member1 = new Member("member1",10);
        Member member2 = new Member("member1",20);
        memberJPARepository.save(member1);
        memberJPARepository.save(member2);

        List<Member> result = memberJPARepository.findByUsernameAndAgeGreaterThan("member1", 15);
        System.out.println(result);
        assertThat(result.get(0).getUsername()).isEqualTo(member1.getUsername());
        assertThat(result.get(0).getAge()).isEqualTo(member2.getAge());
        assertThat(result.size()).isEqualTo(1);

    }
    @Test
    public void testNamedQuery() {
        Member meber1 = new Member("AAA",10);
        Member meber2 = new Member("BBB",20);

        memberJPARepository.save(meber1);
        memberJPARepository.save(meber2);

        List<Member> result = memberJPARepository.findByUsername("AAA");
        Member findMember1 = result.get(0);
        assertThat(findMember1).isEqualTo(meber1);
    }


}

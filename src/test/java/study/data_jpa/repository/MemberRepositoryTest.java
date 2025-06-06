package study.data_jpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.data_jpa.dto.MemberDto;
import study.data_jpa.entity.Member;
import study.data_jpa.entity.Team;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Transactional //Transactional + SpringBootTest = 테스트가 끝나면 롤백(DB에는 아무일도 안일어남)
@Rollback(false) // 테스트에서 수행한 DB 변경 사항이 커밋되어(성공하면) 실제 DB에 반영되도록 설정 (디버깅이나 확인 목적)로
class MemberRepositoryTest {
    // 생성자주입
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    TeamRepository teamRepository;
    @PersistenceContext
    EntityManager em;


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

    @Test
    public void findByUsernameAndAgeGreaterThan() {
        Member member1 = new Member("member1", 10);
        Member member2 = new Member("member1", 20);
        memberRepository.save(member1);
        memberRepository.save(member2);

        List<Member> result = memberRepository.findByUsernameAndAgeGreaterThan("member1", 15);
        System.out.println(result);
        Assertions.assertThat(result.get(0).getUsername()).isEqualTo(member1.getUsername());
        Assertions.assertThat(result.get(0).getAge()).isEqualTo(member2.getAge());
        Assertions.assertThat(result.size()).isEqualTo(1);

    }

    @Test
    public void testNamedQuery() {
        Member member1 = new Member("member1", 10);
        Member member2 = new Member("member2", 20);

        memberRepository.save(member1);
        memberRepository.save(member2);

        List<Member> result = memberRepository.findByUsername("member1");
        Member findMember = result.get(0);
        assertThat(findMember).isEqualTo(member1);
    }

    @Test //레포지토리 매소드에 쿼리 정의 테스트
    public void testQuery() {
        Member member1 = new Member("member1", 10);
        Member member2 = new Member("member2", 20);
        memberRepository.save(member1);
        memberRepository.save(member2);
        List<Member> result = memberRepository.findUser("member1", 10);

        assertThat(result.get(0).getUsername()).isEqualTo(member1.getUsername());
        assertThat(result.get(0)).isEqualTo(member1);
    }

    @Test
    public void findUsernameList() {
        Member member1 = new Member("member1", 10);
        Member member2 = new Member("member2", 20);
        memberRepository.save(member1);
        memberRepository.save(member2);
        List<String> findUsername = memberRepository.findUsernameList();
        for (String s : findUsername) {
            System.out.println(s);
        }
    }
    //@Query의 값 DTO 조회

    @Test
    public void findMemberDto() {
        Member member1 = new Member("member1", 10);
        memberRepository.save(member1);

        Team team1 = new Team("teamA");
        member1.setTeam(team1);
        teamRepository.save(team1);

        List<MemberDto> memberDTO = memberRepository.findMemberDTO();
        for (MemberDto dto : memberDTO) {
            System.out.println(dto);
        }

    }

    @Test
    public void findByName() {
        Member member1 = new Member("member1", 10);
        memberRepository.save(member1);

        Member member2 = new Member("member2", 20);
        memberRepository.save(member2);

        List<Member> result = memberRepository.findyNames(Arrays.asList("member1", "member2"));
        for (Member member : result) {
            System.out.println(member);
        }
    }

    @Test
    public void returnType() {
        Member member1 = new Member("member1", 10);
        Member member2 = new Member("member2", 20);

        memberRepository.save(member1);
        memberRepository.save(member2);

        Member findMember = memberRepository.findMemberByUsername("member1");
        System.out.println(findMember);

        Optional<Member> optionalFindMember = memberRepository.findOptionalByUsername("member1");
        System.out.println(optionalFindMember);
    }
    @Test
    public void paging(){
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 10));
        memberRepository.save(new Member("member3", 10));
        memberRepository.save(new Member("member4", 10));
        memberRepository.save(new Member("member5", 10));

        int age = 10;
        PageRequest pageRequest = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "username"));

        //when
        Page<Member> page = memberRepository.findByAge(age, pageRequest);

        //then
        List<Member> content = page.getContent();
        long totalElements = page.getTotalElements();
        for (Member member : content) {
            System.out.println("member = " + member);
        }
        System.out.println("totalElements = " + totalElements);
        assertThat(content.size()).isEqualTo(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getNumber()).isEqualTo(0);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }
    @Test
    public void bulkUpdate() throws Exception{
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 19));
        memberRepository.save(new Member("member3", 20));
        memberRepository.save(new Member("member4", 21));
        memberRepository.save(new Member("member5", 40));

        assertThat(memberRepository.bulkAgePlus(20)).isEqualTo(3); //21
    }
    @Test
    public void findMemberLazy(){
        //given
        //Member1 -> TeamA
        //Member2 ->
        Team teamA = new Team("TeamA");
        Team teamB = new Team("TeamB");
        teamRepository.save(teamA);
        teamRepository.save(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 10, teamB);
        memberRepository.save(member1);
        memberRepository.save(member2);

        em.flush(); //DB에 반영
        em.clear(); //영속성 컨텍스트 초기화

        //when
        List<Member> members = memberRepository.findAll();
        for (Member member : members) {
            System.out.println(member);
            System.out.println("member.getTeam().getClass() = " + member.getTeam().getClass());
            System.out.println("member.getTeam().getName() = " + member.getTeam().getName()); //Lazy Loading으로 N+1 문제 발생
        }
        List<Member> findMember = memberRepository.findMemberFetchJoin();
        for(Member member : findMember){
            System.out.println("member.getTeam().getClass()" + member.getTeam().getClass());
            System.out.println("member.getTeam().getName() = " + member.getTeam().getName());
        }
        List<Member> findMemberEntityGraph = memberRepository.findMemberEntityGraph();
        for(Member member : findMemberEntityGraph){
            System.out.println("member.getTeam().getClass()" + member.getTeam().getClass());
            System.out.println("member.getTeam().getName() = " + member.getTeam().getName());
        }

    }
    @Test
    public void queryHint(){
        Member member1 = new Member("member1", 10);
        memberRepository.save(member1);
        em.flush();
        em.clear();

        //when
        Member findMember = memberRepository.findReadOnlyByUsername("member1");
        findMember.setUsername("member2");

        em.flush(); //변경 감지 DirtyChecking
    }
    @Test
    public void lock(){
        //given
        Member member1 = new Member("member1", 10);
        memberRepository.save(member1);
        em.flush();
        em.clear();

        List<Member> result = memberRepository.findLockByUsername("member1");
    }
}

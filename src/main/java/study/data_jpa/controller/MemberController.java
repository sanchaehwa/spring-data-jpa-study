package study.data_jpa.controller;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import study.data_jpa.entity.Member;
import study.data_jpa.repository.MemberRepository;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;

    @GetMapping("/members/{id}")
    public String findById(@PathVariable("id") Long id) {
        Member member = memberRepository.findById(id).get();
        return member.getUsername();
    }
    @GetMapping("/members2/{id}")
    public String findMember2(@PathVariable("id") Member member) {
        return member.getUsername();
    }
    @GetMapping("/members")
    public Page<Member> list(Pageable pageable) { //Pagable : Page 나온 파라미터 정보, Page:결과 정보
        Page<Member> page = memberRepository.findAll(pageable);
        return page;
 //Page Request 객체를 생성해서
    }
    @PostConstruct
    public void init() {
       for (int i = 0; i < 100; i++) {
           memberRepository.save(new Member("user"+i,i));
       }
    }
}

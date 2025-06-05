package study.data_jpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import study.data_jpa.entity.Member;

import java.util.*;

@Repository
public class MemberJPARepository {
    @PersistenceContext
    private EntityManager em; //엔티티 메니저

    //저장
    public Member save(Member member) {
        em.persist(member);
        return member;
    }
    //삭제
    public void delete(Member member) {
        em.remove(member);
    }
    //전체 조회
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }
    //Member ID 조회
    public Optional<Member> findById(Long id) {
        Member member = em.find(Member.class, id);
        return Optional.ofNullable(member);
    }
    //Member 객체 개수
    public long count() {
         return em.createQuery("select count(m) from Member m", Long.class).getSingleResult();
    }
    public Member find(Long id) {
        return em.find(Member.class, id);
    }

    public List<Member> findByUsernameAndAgeGreaterThan(String username, int age) {
        return em.createQuery(
                        "select m from Member m where m.username = :username and m.age > :age", Member.class)
                .setParameter("username", username)
                .setParameter("age", age)
                .getResultList();
    }
    public List<Member> findByUsername(String username) {
        return em.createNamedQuery("Member.findByUsername", Member.class)
                .setParameter("username", username)
                .getResultList();
    }
    public List<Member> findByPage(int age, int offset, int limit){
       return em.createQuery("select m from Member m where m.age = :age order by m.username desc", Member.class)
                .setParameter("age", age)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
    public long totalCount(int age){
        return em.createQuery("select count(m) from Member m where m.age = :age",Long.class )
                .setParameter("age",age)
                .getSingleResult();
    }
    //회원의 나이 변경
    public int bulkAgePlus(int age){
        return em.createQuery(
                "update Member m set m.age = m.age + 1" +
                        "where m.age >= :age")
                .setParameter("age",age)
                .executeUpdate();
    }



}


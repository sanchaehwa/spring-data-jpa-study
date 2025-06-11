package study.data_jpa.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.apache.catalina.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import study.data_jpa.dto.MemberDto;
import study.data_jpa.entity.Member;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
@Repository

public interface MemberRepository extends JpaRepository<Member, Long>,MemberRepositoryCustom,JpaSpecificationExecutor<Member> {
    List<Member> findByUsernameAndAgeGreaterThan(String username, int age);

    //@NamedQuery 가 없으면, 매서드 이름으로 쿼리 생성하는 부분으로 실행이 됨.
    List<Member> findByUsername(@Param("username") String username);

    //Repository 매소드에 쿼리 정의
    //이름 없는 NamedQuery 봐도 무방
    @Query(" select m from Member m where m.username = :username and m.age = :age ")
    List<Member> findUser(@Param("username") String username, @Param("age") int age);

    @Query("select m.username from Member m")
    List<String> findUsernameList();

    //DTO로 조회
    @Query("select new study.data_jpa.dto.MemberDto (m.id, m.username, t.name ) from Member m join m.team t")
    List<MemberDto> findMemberDTO();

    //컬렉션 파라미터 바인딩
    @Query("select m from Member m where m.username in  :names")
    List<Member> findyNames(@Param("names") Collection<String> names);

    List<Member> findListByUsername(String username); //컬렉션

    Member findMemberByUsername(String username); //단건

    Optional<Member> findOptionalByUsername(String username); //단건 (값이 있는 경우, 없는경우 NPE 방지)

    @Query(value = "select m from Member m left join m.team t",
            countQuery = "select count(m.username) from Member m")
    Page<Member> findByAge(int age, Pageable pageable);

    //벌크성 수정 쿼리 * 회원 나이 변경
    @Modifying
    @Query("update Member m set m.age = m.age + 1 where m.age>=:age")
    int bulkAgePlus(@Param("age") int age);

    @Query(" select m from Member m left join fetch m.team ")
    List<Member> findMemberFetchJoin();

    //엔티티 그래프
    @Override
    @EntityGraph(attributePaths = {"team"})
    List<Member> findAll();

    @EntityGraph(attributePaths = {"team"})
    @Query(" select  m from Member m ")
    List<Member> findMemberEntityGraph();

    @QueryHints(value = @QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Member findReadOnlyByUsername(String username);

    //Lock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Member> findLockByUsername(String username);

    <T> List<T> findProjectionsByUsername(@Param("username") String username, Class<T> type);

    @Query(value = "select * from member where username = ?", nativeQuery = true)
    Member findByNativeQuery(String username);

    //Projections 활용 , 네이티브 쿼리 + 인터페이스 기반 Projections 활용
    @Query(value = "SELECT m.member_id as id, m.username, t.name as teamName " +
            "FROM member m left join team t ON m.team_id = t.team_id",
            countQuery = "SELECT count(*) from member",
            nativeQuery = true)
    Page<MemberProjection> findByNativeProjection(Pageable pageable);
}

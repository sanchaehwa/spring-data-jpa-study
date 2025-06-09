[복습] 필드 주입 / 생성자 주입

```java
@Autowired
MemberRepository memberRepository;
```

- Setter 나 리플랙션이 필요, 의존성 주입이 어려움.
- 불변성 보장 안됨(final 못 붙임)
- 스프링 컨테이너가 없으면 작동 안함
- 숨겨진 주입이라 가독성이 떨어짐.

```java
@RequiredArgsConstructor 
private final MemberRepository memberReposiotry;
```

- **불변성 보장** (final)
- **테스트하기 쉬움** (생성자로 직접 주입 가능)
- **스프링 권장 방식** (의존성 명시적 주입)
- @Autowired 생략 가능 (스프링이 자동으로 생성자 주입)

## 예제 도메인 모델과 동작확인

```java
package study.data_jpa.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id","username","age"})
@Table(name="members")
public class  Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name ="member_id")
    private Long id;

    @Column
    private String username;

    @Column
    private int age;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    public Member(String username) {
        this.username = username;
    }

}

```

- ToString (of = {”id”, “username”, “age})
    - 기본적으로 모든 필드를 문자열로 출력함 (ToString)
    - (ToString (of )) 로 설정하면 지정한 필드만 문자열로 출력됨
    - team은 양방향 연관관계에 있기 때문에 @ToString에 포함되면 순환 호출(무한 루프)이 발생할 수 있음 그래서 team을 제외한 필드만 문자열로 출력,

## 공통 인터페이스 기능

[복습] JPA

- JPA(Java Persistence API)의 약자로 자바 객체(엔티티)와 관계형 데이터베이스 사이를 연결해주는 ORM 기술. (ORM이란, 객체 , 관계형 DB 자동 매핑)

### 순수 JPA 기반 레포지토리

```java
    public Optional<Team> findById(Long id) {
        Team team = em.find(Team.class, id);
        return Optional.ofNullable(team);
    }
```

- em.find() : 영속성 컨텍스트(1차캐시)에서 해당 ID의 엔티티를 찾고, 영속성 컨텍스트에 없는 경우, DB에 쿼리를 보내서 해당 엔티티를 가지고 옴. **만약 DB에도 없다면 Null 반환**
- DB 에서 가져온 엔티티는 영속성 컨텍스트에 저장이 됨.
- Null 반환하는 경우, Optional.of() 하면 `NullPointerException` 발생.
- 만약, Optional.ofNullable()으로 설정하면, Null를 반환한다 하더라도, Optional.empty() 반환, `NullPointerException` 피해갈 수 있음
- Optional을 사용하는 이유는 값이 있을수도 없을수도 있음.

### 기존팀과 관계 제거

- 팀 A : [멤버1, 멤버2]
- 멤버 1 → 팀A 소속

<aside>

만약에 여기서, 멤버1의 팀을 변경하고자 하면,

`member1.changeTeam(teamB);` 

이렇게 하면, 내부적으로 

`this.team.getMembers().remove(this); // teamA.getMembers().remove(member1`

팀 A의 members 리스트에서 Member1을 제거 . 

그 다음 팀 B의 소속으로 바꾸기 위해 

`teamB.getMembers().add(this);`

- JPA 내부 영속성 컨텍스트(DB)는 member.team만 보고 업데이트
- 그러면 DB에는 member1이 teamB로 변경되지만, Java 메모리상 teamA.getMembers()에는 여전히 member1이 남아있게 됨.

→ **teamA 객체 자체는 삭제되지 않지만**, teamA의 members 목록에 member1이 계속 남아있는 건 
**데이터 일관성 위반**

=기존 팀과의 연관관계를 메모리에서 제거해야함.

</aside>

```java
   public void changeTeam(Team newteam) {
        if(this.team != null) {
            this.team.getMembers().remove(this); //기존 팀과의 관계제거(새로운팀으로 설정하기 위해)
        }
        this.team = newteam;
        newteam.getMembers().add(this); //새로운 팀에 추가
    }
```

### 공통 인터페이스 설게

```java
public interface TeamRepository extends JpaRepository<Team, Long> {
}
```

- Spring Data JPA는 이 인터페이스를 보고 **자동으로 구현체를 생성하여 Bean으로 등록**한다.
- 실제 생성되는 구현체는 SimpleJpaRepository를 기반으로 하며, 내부적으로 JPA의 기본 CRUD 기능을 제공한다.
- 따라서 JpaRepository를 extends 하면, 기본적인 데이터 접근 메서드(ex. findById, save, delete 등)가 자동으로 구현된다.
- 이 구현체는 **프록시 객체**로 생성되며, 인터페이스를 기반으로 만들어진 이 프록시는 내부적으로 SimpleJpaRepository의 메서드를 호출한다.

**Spring은 인터페이스냐, 클래스냐에 따라 두 가지 프록시 방식 중 하나를 사용**

| **조건** | **사용하는 프록시 방식** | **설명** |
| --- | --- | --- |
| interface 기반 Repository | **JDK 동적 프록시** | 인터페이스를 구현하는 프록시 객체 생성 |
| class 기반 Repository | **CGLIB 프록시** | 실제 클래스를 상속해서 프록시 객체 생성 |

## 쿼리 매소드

### 스프링 데이터 JPA가 제공하는 쿼리 매소드 기능

- 자동으로 SQL 쿼리를 만들어준다.

```java
Member findByName(String name);
//SELECT * FROM member WHERE name = ?
//엔티티
@Column
String name;
```

- 조회

| **키워드** | **설명** | **예시** |
| --- | --- | --- |
| findBy | 일반적인 조회 | findByUsername(String name) → 이름으로 조회 |
| readBy | 읽기(조회와 동일) | readByEmail(String email) |
| getBy | 가져오기 (조회와 비슷함) | getById(Long id) |
| queryBy | find와 비슷, SQL 의미 강조 | queryByTitle(String title) |
- 조건절

```java
findByUsernameAndAge(String username, int age)
//WHERE Username = ? AND Age =?
//엔티티 
@Column
String username;
@Column
int age;

//서비스 
User user = findByUsernameAndAge("Kim", 23)
//Kim 23살 인 User 찾고, user 객체로 리턴
```

- Count

| **키워드** | **반환 타입** | **예시** |
| --- | --- | --- |
| countBy | long | countByEmail(String email) → 같은 이메일 몇 |
- EXISTS (존재여부)

| **키워드** | **반환 타입** | **예시** |
| --- | --- | --- |
| existsBy | boolean | existsByPhoneNumber(String phone) → 전화번호 있는지 확인 |
- DELETE(삭제)

| **키워드** | **반환 타입** | **예시** |
| --- | --- | --- |
| deleteBy, removeBy | 삭제된 행 수 (long) | deleteByUsername(String name) → 이름으로 삭제 |
- DISTINCT(중복제거)

| **키워드** | **설명** | **예시** |
| --- | --- | --- |
| findDistinct, findMemberDistinctBy | 중복 제거 | findDistinctByUsername(String name) |
- LIMIT(제한)

|  | **설명** | **예시** |
| --- | --- | --- |
| findFirst3 | 처음 3개 | findFirst3ByOrderByIdDesc() → ID 기준 역순 정렬해서 3개 |
| findTop | findFirst와 동일 | findTopByAge(int age) |
| findTop3 | 상위 3개 | findTop3ByAgeGreaterThan(20) |

### NamedQuery

- JPQL(JPA Query Language) 미리 정의해놓고 이름을 붙여 사용하는 쿼리.
- 주로 복잡한 쿼리, 자주 사용하는 쿼리를 미리 선언 해놓고 재 사용할 때 사용.
- **정적 쿼리**이기 때문에, **애플리케이션 실행 전에 문법 오류를 컴파일 타임에 체크**할 수 있어 안정적.

```java
@Entity
@NamedQuery(
    name = "Member.findByUsername",
    query = "SELECT m FROM Member m WHERE m.username = :username"
)//@NamedQuery 가 없으면, 매서드 이름으로 쿼리 생성하는 부분으로 실행이 됨.
public class Member {
    ...
}

...
TypedQuery<Member> query = em.createNamedQuery("Member.findByUsername", Member.class);
query.setParameter("username", "kim");
List<Member> result = query.getResultList();
```

### 컬렉션 파라미터 바인딩

- JPQL에서 IN절에 컬렉션 타입을 바인딩 할 수 있음.
- 즉, 파라미터로 **List, Set 등의 컬렉션을 전달**하면 IN 조건을 만족하는 항목을 조회 가능.

```java
SELECT m FROM Member m WHERE m.name IN :names

query.setParameter("names", Arrays.asList("kim", "lee", "park"));
```

[참고] NPE(NullPointerException) / Optional

- NullPointerException(NPE) 방지를 위해 **null을 직접 다루지 않고**, 대신 **Optional<T>로 감싸서 처리**
- Optional<T>는 **T 타입의 값이 “있을 수도 있고, 없을 수도 있음”을 명시**하는 **Wrapper 클래스**

[참고] Optional은 언제 쓰이는가.

- 단일 객체를 반환할 때, 그 객체가 있을수도 있고, 없을 수도 있다면, Optional<T>를 사용하는 것이 적절.

| **상황** | **Optional 사용 여부** | **이유** |
| --- | --- | --- |
| 단일 객체 반환 | 권장 | null일 수도 있음을 명확하게 표현 |
| 컬렉션 반환 | 비권장 | 빈 컬렉션 반환이 더 직관적 ([]) |
| 파라미터로 사용 | 비권장 | 오히려 복잡해지고 가독성 저하 |
| 엔티티 필드로 사용 | 비권장 | JPA 등 ORM이 인식 못할 수 있음 |

### 순수 JPA 페이징 쿼리

<aside>

검색 조건: 나이가 10살
정렬 조건: 이름으로 내림차순
페이징 조건: 첫 번째 페이지, 페이지당 보여줄 데이터는 3건

</aside>

```java
// 실제 데이터 조회 시 정렬을 포함해야 사용자에게 정렬된 결과 제공 가능
public List<Member> findByPage(int age, int offset, int limit) {
    return em.createQuery("select m from Member m where m.age = :age order by m.username desc", Member.class)
            .setParameter("age", age)
            .setFirstResult(offset)   // 시작 위치 (페이징 offset)
            .setMaxResults(limit)     // 조회할 데이터 수 (페이징 limit)
            .getResultList();
}

// 데이터 개수만 계산하는 쿼리이므로 정렬은 불필요 (성능 최적화를 위해 정렬 조건 제거)
public long totalCount(int age) {
    return em.createQuery("select count(m) from Member m where m.age = :age", Long.class)
            .setParameter("age", age)
            .getSingleResult();
}
```

### 스프링 데이터 JPA 페이징과 정렬

**페이징과 정렬 파라미터**

`org.springframework.data.domain.Sort`: 정렬 기능
`org.springframework.data.domain.Pageable:` 페이징 기능 (내부에 Sort 포함)

**특별한 반환 타입**

`org.springframework.data.domain.Page`: 추가 count 쿼리 결과를 포함하는 페이징

`org.springframework.data.domain.Slice`: 추가 count 쿼리 없이 다음 페이지만 확인 가능 (내부적으로 limit + 1 조회)

`List` (자바 컬렉션): 추가 count 쿼리 없이 결과만 반환

**정렬과 페이징**

- Sort : 정렬 조건을 설정할 수 있음 (ex) 이름 내림차순, 나이 내림차순

```java
Sort.by(Sort.Direction.DESC, "username")
```

- Pageable
    - 페이징 정보를 담는 객체 (페이지 번호, 페이지 크기, 정렬조건 포함)
    - 페이지 단위로 데이터를 나눠서 조회할 수 있음.
    - 내부적으로 Sort도 포함할 수 있음

```java
PageRequest.of(0, 10, Sort.by("username").descending()) // 0페이지부터 10개씩 username 내림차순
```

| **반환 타입** | **count 쿼리** | **목적** | **장점** | **단점** |
| --- | --- | --- | --- | --- |
| Page<T> | 실행함 | **정확한 전체 페이지 계산** | 전체 페이지 수, 현재 페이지, 총 데이터 수 등 풍부한 정보 | count 쿼리 비용이 큼 |
| Slice<T> | 실행 안 함 | **“다음 페이지 있음?” 확인 용도** | 다음 페이지 있는지 빠르게 판단 (limit+1 방식) | 전체 페이지 수는 알 수 없음 |
| List<T> | 실행 안 함 | 그냥 리스트만 필요할 때 | 단순 결과만 빠르게 | 페이징 관련 정보 없음 |

예시

```java
//MemberRepository.java
Page<Member> findByUsernameAndAge(String username,Integer age, Pageable pageable);
//Integer은 Null 허용, int Null이 허용되지않는 원시타입이기에, 이 age는 추가 필터링 조건이라 = 선택사항 Null 허용
//MemberSearchRequest : DTO
//findByUsernameAndAge -고정 조건, 여러테이블을 조인해야하거나, 복잡하거나, 조건이 선택적으로 들어오면 Querydsl @Query를 쓰는것이 좋음

public class MemberSearchRequest {
    private String username;     // 검색 조건
    private Integer age;         // 추가 필터링 조건
    private int page = 0;        // 페이지 번호 (0부터 시작)
    private int size = 10;       // 페이지 당 개수 (데이터의 개수)
    private String sort = "createdDate,DESC"; // 정렬 조건 * 내림차순
}

//MemberController.java
@GetMapping("/members") 
public ResponseEntity<Page<MemberDto>> getMembers(MemberSearchRequest req) {
    Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), 
        Sort.by(Sort.Direction.fromString(req.getSort().split(",")[1]), //정렬 방향
                req.getSort().split(",")[0])); //정렬 기준 필드

    Page<MemberDto> result = memberService.searchMembers(req, pageable);
    return ResponseEntity.ok(result);
}

/*
?page=0&size=10&sort=createdDate,DESC -> Request
0번째 페이지 (처음페이지) 10개씩, DESC (내림차순) , createdDate 기준
*/

//MemberService.java
public Page<MemberDto> searchMembers(MemberSearchRequest req, Pageable pageable) {
    return memberRepository.search(req.getUsername(), req.getAge(), pageable)
                           .map(MemberDto::from); 
}
```

### CountQuery 분리

**Query 어노테이션**

- Spring Data JPA는 @Query 어노테이션을 통해 **JPQL로 직접 쿼리를 정의**할 수 있음
- Page<Member>와 같이 **Page 타입을 반환**하면, 내부적으로는 다음 두 쿼리를 실행
    1. 실제 데이터를 가져오는 **content 쿼리**
    2. 전체 개수를 계산하는 **count 쿼리**

**count 쿼리는 left join 없이 따로 쓰는지,**

- Page<Member>는 **페이징을 위한 전체 건수(count)** 도 필요하기 때문에 count 쿼리도 실행된다.
- 그러나 left join을 사용하면:
    - 조인된 테이블의 데이터로 인해 **row가 중복**될 수 있고,
    - 이 중복이 **count 결과를 부정확하게 만들거나 성능을 저하시킬 수 있음**
- 특히, fetch join이 포함된 JPQL에서는 **JPA가 count 쿼리를 자동 생성할 수 없음**
    - 이 경우 **직접 countQuery를 명시**해야 함.

> 실무에서는 쿼리를 분리함으로써, 성능 최적화 함.
> 

```java
@Query(
  value = "select m from Member m left join m.team t", 
  countQuery = "select count(m.username) from Member m"
)
Page<Member> findByAge(int age, Pageable pageable)

//content 쿼리에는 조인을 포함하고, count 쿼리에는 불필요한 조인을 제거 count는 단순하게 가져오는 전략

```

[참고] LEFT JOIN

- 왼쪽 테이블의 모든 행을 유지하면서, 오른쪽에 해당하는 데이터가 있으면 가져오고, 없으면 NULL 로 처리

| **id** | **username** | **team_id** |
| --- | --- | --- |
| 1 | aaa | 1 |
| 2 | bbb | 2 |
| 3 | ccc | NULL |

→LEFT JOIN 하면

| **username** | **team_name** |
| --- | --- |
| aaa | 팀A |
| bbb | 팀B |
| ccc | NULL |

### 벌크성 수정 쿼리

**벌크연산(DML)**

- 벌크 연산이란, 한번의 쿼리로 여러 엔티티를 수정하거나 삭제하는 작업

(예) 모든 회원의 나이를 한꺼번에 증가시키는 쿼리

```java
@Modifying
@Query("update Member m set m.age = m.age + 1 where m.age >= :age")
int bulkUpdate(@Param("age") int age);
```

- @Modifying이 필요한 이유.
    - JPA의 @Query는 기본적으로 select용(조회) 설계되어 있음.
    - update, delete 쿼리를 쓰려면 DML(데이터 변경 쿼리)임을 명시해줘야 함.
    - 그래서 @Modifying이 필요함.
- 벌크연산(DML)은 영속성 컨텍스트를 무시하고 DB에 직접 반영되기에, 영속성 컨텍스트 초기화가 필요함.

```java
// 1. findById로 Member를 조회 -> 영속성 컨텍스트에 올라감
Member member = memberRepository.findById(1L).get(); // age: 20

// 2. 벌크 업데이트 실행
memberRepository.bulkUpdate(18); // age >= 18인 회원 age +1 (DB에는 21이 됨)

// 3. 다시 조회하면? → 영속성 컨텍스트에 있는 20이 보임 - 변경된 값으로 보여야하는데 - 문제
System.out.println(member.getAge()); 

```

> 그래서 영속성 컨텍스트를 초기화해주고, 벌크 연산 직후에 자동 초기화를 시켜서 다시 DB에서 조회를 해서 가져오거나, 초기화 (findById, findAll)를 벌크 연산보다 먼저 실행해야함.
> 

```java
@Modifying(clearAutomatically = true) //벌크 쿼리 실행 후 , 영속성 컨텍스트 자동 초기화
@Query("update Member m set m.age = m.age + 1 where m.age >= :age")
int bulkUpdate(@Param("age") int age);
```

### @EntityGraph

[복습]

**지연로딩 - 즉시로딩** 

| **구분** | **즉시로딩 (EAGER)** | **지연로딩 (LAZY)** |
| --- | --- | --- |
| 정의 | 엔티티를 조회할 때 **연관된 엔티티들도 즉시 같이 조회** | 엔티티를 조회할 때 **연관된 엔티티는 프록시로 남겨두고 나중에 필요할 때 조회** |
| 쿼리 | 기본 엔티티 + 연관 엔티티를 **즉시 JOIN 해서 가져옴** | 기본 엔티티만 가져오고, **연관 엔티티는 나중에 필요할 때 쿼리** |
| 장점 | 코딩은 간편 (연관 엔티티까지 이미 로딩됨) | 성능 최적화 가능 (필요할 때만 쿼리 보내니까) |
| 단점 | **불필요한 데이터까지** 항상 다 불러옴 → 성능 저하 가능성 | 트랜잭션 관리 실수하면 **LazyInitializationException** 터질 수 있음 |
| 사용 경우 | 연관 엔티티를 항상 같이 써야 할 때 | 연관 엔티티를 가끔만 필요할 때 |

```java
System.out.println("member.getTeam().getName() = " + member.getTeam().getName()); : member.getTeam() <- 이거는 쿼리안나감, member.getTeam().

```

`member.getTeam()` : 쿼리 안나감. Team 엔티티를 직접가져오는것이 아니라, 프록시 객체만 반환.

`member.getTeam().getName()` : getName() 호출하는 순간, 프록시가 초기화가 되면서 Team 데이터를 DB에서 조회 →쿼리 나감 

> 여러 Member가 있다면, 루프를 돌면서 member.getTeam().getName()을 호출할 때마다 Team을 개별적으로 조회하게 되어 **N+1 문제**

이는 @ManyToOne(fetch = LAZY)로 인해 Team 데이터를 **필요할 때 지연 로딩 하기 때문.** 이 문제를 해결하기 위해 **Fetch Join, EntityGraph, DTO Projection 한 번의 쿼리로 필요한 데이터모두 가져오는 방식이 성능상 효율적.**
> 

### Fetch Join

```java
    @Query(" select m from Member m left join fetch m.team ")
    List<Member> findMemberFetchJoin();
    
    /*
    selct m from Member m : Member 엔티티 기준으로 조회
    left join : 팀 없어도 member는 조회(null 허용)
    fetch :JPA 가 연관된 Team 객체도 한번에 메모리에 로딩하도록 
    * 필요한 데이터를 한번에 조회하니 필요할때마다 가져다 씀으로써 발생한 N+1 문제 해결하고
    한번만 쿼리를 날릴 수 있음
    */
```

- JPQL을 사용하여 Member와 관련된 Team을 함께 조회(Fetch Join)하는 쿼리.

```java
class study.data_jpa.entity.Team
```

- class study.data_jpa.entity.Team : 실제 Team 객체 (프록시 아님, 초기화 완료)
- class study.data_jpa.entity.Team$HibernateProxy$... : Hibernate가 만든 프록시 객체 
**지연로딩 상태
1. **Fetch Join 사용 시**
    - JPQL에서 fetch 키워드를 사용하면, 연관 엔티티를 **즉시 로딩(EAGER)처럼 동작**하게 만듦.
    - 따라서 **프록시 객체가 아니라 실제 객체가 즉시 메모리에 로딩**됨.
2. **즉시 로딩(EAGER) 설정 시**
    - 연관된 엔티티를 **엔티티 조회 시점에 바로 DB에서 함께 조회**.
    - 이 또한 프록시가 아닌 **실제 객체로 로딩됨**.
- 반대로 지연로딩이면, 처음에 프록시 객체를 대신 주고, `getName()` 처럼 실제로 해당 객체의 필드를 사용할때, DB 쿼리 실행해서 진짜 데이터를 채운다.

### 엔티티 그래프

```java
    @EntityGraph(attributePaths = {"imageSet"})
    @Query("select b from Booth b where b.bno =:bno")
    Optional<Booth> findByIdWithImages(@Param("bno") Long bno);
```

- Booth 엔티티를 bno 기준으로 조회할 때, Booth와 연관된 imageSet도 한 번의 쿼리로 함께 가져옴.
- JPA에서 지연 로딩(Lazy Loading)으로 설정된 연관 엔티티를 즉시 로딩(Eager Loading)처럼 특정 쿼리에서만 함께 로딩하기 위해 JPQL을 수정하지 않고도 지정할 수 있도록 해주는 기능이 @EntityGraph
- 즉, 기본은 지연 로딩을 유지하되, 특정 쿼리에서만 지정된 연관 엔티티(imageSet)를 함께 가져오고 싶을 때 사용.
- 이는 필요할 때마다 객체를 불러옴으로써 발생하는 N+1 문제를 해결할 수 있으며, Fetch Join처럼 동작하지만 코드가 더 깔끔하고 엔티티 중심으로 작성할 수 있어 유지보수가 수월

### JPA Hint & Lock

JPA Hint & Lock 기능은 성능 최적화와 동시성 제어를 위해 사용함.

[참고] 동시성 제어

- 여러 사용자가 동시에 같은 데이터를 접근하거나 변경할때 발생할 수 있는 충돌을 방지하고 데이터의 일관성을 유지하는것.
- (예) UserA가 수량 1을 결제하려고함, ⇒ UserB도 같은 수량을 결제하려고 하면 
수량은 하나였기에, 두 사람 모두 결제를 완료하면 데이터 오류 발생.
- 이때 동시성 제어로, 한명만 성공하거나, 충돌이 발생했음을 알려야함/

동시성 제어 방식

1. 비관적 락 (Pessimistic Lock)
    - 다른 사람이 이 데이터 건들지 모르기에, 먼저 락을 걸고, 다른 트랜잭션은 대기 or 차단을 함.
    - DB 수준의 락이 걸림
    - 동시에 접근하면 다른 트랜잭션은 대기
    - 충돌 방지는 할 수 있지만, 성능 저하 가능 (DB 수준의 락, 데드락 가능성)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select m from Member m where m.id = :id")
Member findWithLock(@Param("id") Long id);
```

- LockModeType 종류:
    - PESSIMISTIC_READ: 읽기 락
    - PESSIMISTIC_WRITE: 쓰기 락 (가장 자주 사용)
    - PESSIMISTIC_FORCE_INCREMENT: 버전 증가 강제
1. 낙관적 락 (Optimistic Lock)
    - 버전 필드(@Version)를 통해 변경 충돌을 감지하고 예외 처리
    - 트랜잭션 커밋 시점에 version 값이 동일한지 확인
    - 충돌 발생 시 OptimisticLockException 발생

```java
@Entity
public class Product {
    @Id @GeneratedValue
    private Long id;

    @Version // 이 필드를 기준으로 충돌 감지 
    private int version;

    private String name;
}
```

JPA는 트랜잭션 커밋 시점에, Version 컬럼 값을 조건에 넣고 Update 실행.

`update product set name=?, version=? where id=? and version=?`

vesion 값이 바뀌었다 하면, 0건 업데이트 되면서, `OptimisticLockException` 예외 발생

1. JPA Hint

```java
@QueryHints(value = {
    @QueryHint(name = "org.hibernate.readOnly", value = "true")
})
@Query("select m from Member m where m.id = :id")
Member findReadOnly(@Param("id") Long id);
```

- readOnly로 설정하면 Hibernate는 해당 객체를 **변경 감지 하지 않음 (dirty checking X)**
- 성능 최적화용 → 조회 전용 쿼리에 효과적

| **구분** | **비관적 락** | **낙관적 락** | **Query Hint** |
| --- | --- | --- | --- |
| 특징 | DB에서 락 걸기 | 버전 필드로 충돌 감지 | 읽기 최적화 등 |
| 목적 | 충돌 방지 | 충돌 감지 후 롤백 | 불필요한 감지 방지 |
| 성능 | 낮음 (락 비용) | 높음 (락 없음) | 높음 (변경 감지 X) |
| 충돌 발생 시 | 대기 or 차단 | 예외 발생 | 해당 |

## 확장 기능

### 사용자 정의 Repository 구현

- 스프링 데이터 JPA Repository 는 Interface만 정의하고 구현체는 스프링이 자동 생성

```java
public interface UserRepository extends JpaRepository<User,Long>{
		User findByUsername(String username);
}
/*
- UserRepository는 JPA Repository를 상속하고 있으므로, Spring은 내부적으로 프록시 객체를 
만들어서 FindByUsername을 처리할 수 있는 구현체를 자동 생성해줌. 
- 스프링 부팅 시점에 @EnableJpaRepository를 통해 Repositroy 인터페이스들을 스캔
- 각 인터페이스에 대해 JDK 동적 프록시 혹은 CGLB 프록시를 사용해 적절한 쿼리를 생성해서 동작하는 클래스
를 자동 만들어주면
- Service Layer에서도 User user = userRepository.findByUsername("chris");
동작하는것
=> 인터페이스 만 만들면, 스프링에서 자동 생성
*/

```

- 스프링 데이터 JPA가 제공하는 인터페이스를 직접 구현하면 구현해야하는 기능이 너무 많음
    - JpaRepository에 있는 수많은 기능(예: save, delete, findAll 등)을 **전부 직접 구현해야함.**
- 인터페이스 매서드를 직접 구현하고자 한다면
    - JPA 직접 사용(Entity Manager)
    - 스프링 JDBC Template 사용
    - MyBatis 사용
    - 데이터베이스 커넥션 직접 사용
    - QueryDSL 사용

### 도메인 클래스 컨버터

**`@PostConstruct`** 

- Spring Bean이 생성된 직후(의존성 주입까지 완료된 후) 자동으로 호출되는 초기화 매서드를 지정할때 사용하는것
- Return  값이 없어야하며, 파라미터가 없어야함. static 매서드에 붙이면 안됨. 클래스가 SpringBoot여야함.
- 외부 API 연결 초기화 / 설정 값 검증 / 의존성 객체를 활용한 로직 수행에서 쓰임.

```java
@PostConstruct
public void init() {
    memberRepository.save(new Member("userA"));
}
//서버가 처음 켜질때, userA라는 사용자가 DB에 미리 저장이 됨
```

**도메인 클래스 컨버터**

- SpringMVC는 @PathVariable , @RequestParam 등으로 전달받은 값을 해당 엔티티로 자동 변환해주는 기능을 제공하는데, 이 기능이 도메인 클래스 컨버터.
1. /members2/{id}에서 {id}는 문자열이나 숫자(Long 등)로 전달
2. Spring은 해당 ID 값을 기반으로 Member 엔티티를 DB에서 조회하려고 시도.
3. 이때 Spring Data JPA가 내부적으로 JpaRepository의 findById()를 자동 호출해서 Member 객체로 바인딩해줌.

### **도메인 클래스 컨버터의 한계**

- 컨트롤러 레이어에서만 사용 가능 (Service, Repository 레이어에서는 비추)
- ID로 DB에서 조회하는 단순 케이스에만 적합
- 복잡한 로직이 들어갈 경우 명시적으로 findById() 호출하는 것이 좋음

### 페이징과 정렬

- 페이징을 바탕으로 한 데이터 조회에 대한 요청은 PageRequest 라는 클래스로 치환하여 기능을 구현할 수 있음. Pageable 인터페이스를 구현할 구체 클래스 = PageRequest
- 컨트롤러 메서드에서 `Pageable pageable`을 선언하면, **Spring이 자동으로** PageRequest 객체를 생성해서 이 pageable에 **주입(Injection)**
- 이 클래스를 사용하면 DB에서 가져올 데이터의 일부를 특정 페이지로 제한하고,필요에 따라 정렬옵션을 지정할 수 있음
    - 페이징은 사용자가 어떤 데이터를 요청했을 때, 전체 데이터 중 원하는 정렬 옵션에 따라 제공하는 방식

### @PageableDefault

- 클라이언트가 page, size(데이터개수) 혹은 sort(정렬기준) 정보를 쿼리 스트링(입력데이터)로 넘겨주지않으면, 한 페이지에 20개의 사이즈로 분리된 페이지 중 첫 페이지를 반환

```java
public abstract class PageableHandlerMethodArgumentResolverSupport {

    private static final String INVALID_DEFAULT_PAGE_SIZE = "Invalid default page size";

    **private static final String DEFAULT_PAGE_PARAMETER = "page";
    private static final String DEFAULT_SIZE_PARAMETER = "size";
    private static final String DEFAULT_PREFIX = "";
    private static final String DEFAULT_QUALIFIER_DELIMITER = "_";
    private static final int DEFAULT_MAX_PAGE_SIZE = 2000;**

    static final Pageable DEFAULT_PAGE_REQUEST = PageRequest.of(0, 20);

    **private Pageable fallbackPageable = DEFAULT_PAGE_REQUEST;**
    private String pageParameterName = DEFAULT_PAGE_PARAMETER;
    private String sizeParameterName = DEFAULT_SIZE_PARAMETER;
    private String prefix = DEFAULT_PREFIX;
    private String qualifierDelimiter = DEFAULT_QUALIFIER_DELIMITER;
    private int maxPageSize = DEFAULT_MAX_PAGE_SIZE;
}
```

- `@PageableDefault` 어노테이션은 기본 페이징 및 정렬 옵션을 정하는데 사용되는데, 해당 어노테이션을 사용하며 별다른 옵션을 설정하지 않을 시 위 `fallbackPageable` 옵션이 사용
- Default 옵션을 사용하지 않고, 커스텀하면

```java
@GetMapping("/reviews")
public ResponseEntity<ReviewPageResponseDTO> memberReviews(
				**@PageableDefault(
				page = 0, size = 10, sort = "id", direction = Sort.Direction.DESC) 
				Pageable pageable)** {
		Page<Review> reviews = memberService.getReviews(pageable);
		ReviewPageResponseDTO response = ReviewConverter.toReviewPageResponse(reviews);
		return ResponseEntity.ok(response);
	}
}
```

- DefaultPage의 경우 개발자가 정한 기본 Page의 형식이다. 기타 설정을 해주지 않으면 `FallbackPage` 의 설정으로 실행된다. `Fallback` 의 경우, 적합한 방식이 없을 때, 만일을 대비해 만들어둔 설정. DefaultPage는 `@PageableDefault` 어노테이션을 통해 설정함.

### (페이징) 요청 파라미터

```java
/members?page=0&size=3&sort=id,desc&sort=username,desc
```

- Page : 현재 페이지 (0부터 시작)
- Size : 한 페이지에 노출할 데이터 건수
- Sort : 정렬 조건 (ASC | DESC), 정렬 방향을 변경하고 싶으면 `sort`  파라미터 추가 (`asc` 생략 가능 * 기본값이기때문에)

## 스프링 데이터 JPA 구현체 분석

스프링 데이터 JPA가 제공하는 공통 인터페이스의 구현체
`org.springframework.data.jpa.repository.support.SimpleJpaRepository`

```java
@Repository
@Transactional(
    readOnly = true ) //조회
    
//스프링 빈의 컴포넌트 스캔 대상이 되는것 - 스프링이 읽어드려가지고 스프링 컨테이너에 올림
//JPA , JDBC는 예외가 틀린데, 이걸 스프링에서 쓸수있는 예외로 바뀌어서 올라감 - Repository 

public class SimpleJpaRepository<T, ID> implements JpaRepositoryImplementation<T, ID> {
    private static final String ID_MUST_NOT_BE_NULL = "The given id must not be null";
    private static final String IDS_MUST_NOT_BE_NULL = "Ids must not be null";
    private static final String ENTITY_MUST_NOT_BE_NULL = "Entity must not be null";
    private static final String ENTITIES_MUST_NOT_BE_NULL = "Entities must not be null";
    private static final String EXAMPLE_MUST_NOT_BE_NULL = "Example must not be null";
    private static final String SPECIFICATION_MUST_NOT_BE_NULL = "Specification must not be null";
    private static final String QUERY_FUNCTION_MUST_NOT_BE_NULL = "Query function must not be null";
    private final JpaEntityInformation<T, ?> entityInformation;
    private final EntityManager entityManager;
    private final PersistenceProvider provider;
    @Nullable
    private CrudMethodMetadata metadata;
    @Nullable
    private ProjectionFactory projectionFactory;
    private EscapeCharacter escapeCharacter;

    @Transactional //memberRepository.save() 하면 호출됨 - 매서드 레벨에서 @Transaction 을 걸면 이거 먼저 인정이 됨
    public <S extends T> S save(S entity) {
        Assert.notNull(entity, "Entity must not be null");
        if (this.entityInformation.isNew(entity)) {
            this.entityManager.persist(entity); //새로운 엔티티면 저장
            return entity;
        } else {
            return (S)this.entityManager.merge(entity); //새로운 엔티티가 아니면 병합
        }
    }
```

`@Repository` 적용: JPA 예외를 스프링이 추상화한 예외로 변환

`@Transactional` 트랜잭션 적용

- JPA의 모든 변경은 트랜잭션 안에서 동작
- 스프링 데이터 JPA는 변경(등록, 수정, 삭제) 메서드를 트랜잭션 처리
- 서비스 계층에서 트랜잭션을 시작하지 않으면 리파지토리에서 트랜잭션 시작
- 서비스 계층에서 트랜잭션을 시작하면 리파지토리는 해당 트랜잭션을 전파 받아서 사용
- 그래서 스프링 데이터 JPA를 사용할 때 트랜잭션이 없어도 데이터 등록, 변경이 가능했음(사실은 트랜잭션
이 리포지토리 계층에 걸려있는 것임)

`@Transactional(readOnly = true)`

- 데이터를 단순히 조회만 하고 변경하지 않는 트랜잭션에서 `readOnly = true` 옵션을 사용하면 플러시
를 생략해서 약간의 성능 향상을 얻을 수 있음

**  Merge는, **“변경된 필드만” 업데이트**하는 게 아니라 **“모든 필드”를 복사**해서 갱신하는것
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
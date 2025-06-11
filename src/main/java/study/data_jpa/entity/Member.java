package study.data_jpa.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id","username","age"}) //Team을 포함하게되면 무한 순환(양방향연관관계) - Team 제외
@NamedQuery(name= "Member.findByUsername", query = " select m from Member m where m.username = :username") //정적 쿼리, 컴파일 시점에 문법 오류를 잡을수있음.
public class  Member extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name ="member_id")
    private Long id;
    private String username;
    private int age;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public Member(String username,int age, Team team) {
        this.username = username;
        this.age = age;
        if(team != null) {
            changeTeam(team);
        }
    }
    public Member(String username) {
        this.username = username;
    }
    public Member(String username, int age) {
        this.username = username;
        this.age = age;
    }
    public void changeTeam(Team newteam) {
        this.team = newteam;
        newteam.getMembers().add(this); //새로운 팀에 추가
    }


}

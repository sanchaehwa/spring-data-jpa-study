package study.data_jpa.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id","username","age"}) //Team을 포함하게되면 무한 순환(양방향연관관계) - Team 제외
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

    public Member(String username,int age, Team team) {
        this.username = username;
        this.age = age;
        this.team = team;
    }
    public Member(String username) {
        this.username = username;
    }
    public void changeTeam(Team newteam) {
        if(this.team != null) {
            this.team.getMembers().remove(this); //기존 팀과의 관계제거(새로운팀으로)
        }
        this.team = newteam;
        newteam.getMembers().add(this); //새로운 팀에 추가
    }


}

package study.data_jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="teams")
@ToString(of = {"id", "name"})
public class Team extends JpaBaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name ="team_name")
    private String name;

    @OneToMany(fetch = FetchType.LAZY,mappedBy = "team")
    private List<Member> members;

    public Team(String name) {
        this.name = name;
    }


}

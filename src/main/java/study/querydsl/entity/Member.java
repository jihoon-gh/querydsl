package study.querydsl.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@SequenceGenerator(
        name = "member_id_seq",
        sequenceName = "idx_member",
        allocationSize=1
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id","username","age"})
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="team_id")
    private Team team;

    public Member(String username, int age, Team team){
        this.username = username;
        this.age = age;
        if(team != null){
            changeTeam(team);
        }
    }

    public Member(String username){
        this(username,0,null);
    }

    public Member(String username, int age){
        this(username,age,null);
    }

    public void changeTeam(Team team){
        this.team = team;
        team.getMembers().add(this);
    }

}

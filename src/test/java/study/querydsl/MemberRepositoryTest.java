package study.querydsl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.repository.MemberJpaRepository;

@SpringBootTest
@Transactional
public class MemberRepositoryTest {

    @PersistenceContext
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest(){

        //given
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        //when
        Member member1 = memberJpaRepository.findById(member.getId()).get();

        //then
        Assertions.assertThat(member1).isEqualTo(member);
    }
}

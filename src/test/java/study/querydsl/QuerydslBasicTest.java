package study.querydsl;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        //member1 찾기
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();


        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();


        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username
                        .eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch() {

        List<Member> members = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();
    }

    /**
     * 회원 이름 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = members.get(0);
        Member member6 = members.get(1);
        Member memberNull = members.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        Long numOfMember = queryFactory
                .select(member.count())
                .from(member)
                .fetchFirst();

        assertThat(numOfMember).isEqualTo(4L);
    }

    @Test
    public void aggregation() {
        List<com.querydsl.core.Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);

    }

    @DisplayName("")
    @Test
    public void group() throws Exception {

        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @DisplayName("")
    @Test
    public void test() throws Exception {
        //given
        List<Member> results = queryFactory
                .select(member)
                .from(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        //when

        //then
        assertThat(results)
                .extracting("username")
                .containsExactly("member1", "member2");

    }

    @DisplayName("회원의 이름이 팀 이름과 같은 회원 조회")
    @Test
    public void theta_join() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamA"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
        //when

        //then

    }


    /**
     * ex) 회원과 팀을 조인하면서, 팀 이름이 teamA인 team만 조인. 회원은 모두 조회
     */

    @DisplayName("")
    @Test
    public void join_on_filtering() throws Exception {
        //given
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA")) //member는 left니까 다 가져옴
                .fetch();
        //when

        //then

    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @DisplayName("")
    @Test
    public void fetchJoinTest() throws Exception {
        //given
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        //when

        //then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).isFalse();

    }

    @DisplayName("")
    @Test
    public void subQuery() throws Exception {

        QMember memberSub = new QMember("memberSub");
        //given
        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        //when

        //then
        assertThat(result)
                .extracting("age")
                .containsExactly(40);
    }

    @DisplayName("")
    @Test
    public void subQueryGoe() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");
        //when
        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        //then

        assertThat(result)
                .extracting("age")
                .containsExactly(30, 40);
    }


    @DisplayName("")
    @Test
    public void basicCase() throws Exception {
        //given
        List<String> result = queryFactory.select(member.age
                        .when(10).then("10살")
                        .when(20).then("20살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        //when

        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @DisplayName("")
    @Test
    public void complexCase() throws Exception {
        //given
        List<String> result = queryFactory.select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        //when

        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    @DisplayName("")
    @Test
    public void constant() throws Exception {
        //given
        List<Tuple> result = queryFactory.select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        //when

        //then
        for (Tuple t : result) {
            System.out.println("t = " + t);
        }

    }

    @DisplayName("")
    @Test
    public void concat() throws Exception {
        //username_age 형태
        List<String> result = queryFactory
                .select(member.username.concat(" _ ").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        //when

        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection() {
        List<String> result = queryFactory.select(member.username)
                .from(member)
                .fetch();

        System.out.println(result);
    }

    @Test
    public void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) { //Tuple은 querydsl 내부 구현이므로, Repository 계층을 넘어가서 사용하지 말자.
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println(username + "  " + age);
        }
    }

    @Test
    public void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println(memberDto);
        }
    }

    @Test
    public void findDtoBySetter(){
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, //기본 생성자를 통해 MemberDto 만들고 세터 세터
                        member.username,
                        member.age))
                .from(member).fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByFields(){
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, //기본 생성자를 통해 MemberDto 만들고 세터 세터
                        member.username,
                        member.age))
                .from(member).fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor(){
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, //기본 생성자를 통해 MemberDto 만들고 세터 세터
                        member.username,
                        member.age))
                .from(member).fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(
                        member.username,
                        member.age))
                .from(member).fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParameter = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember(usernameParameter, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember(String usernameCond, Integer ageCond){
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond!=null){
            builder.and(member.username.eq(usernameCond));
        }
        if(ageCond!=null){
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .select(member)
                .from(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_WhereParam(){
        String usernameParameter = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParameter, ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .select(member)
                .from(member)
                .where(allEq(usernameCond,ageCond))
                .fetch();
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private Predicate allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test //jpa는 영속성 컨텍스트에 엔티티를 관리. 벌크 연산은 영속성 컨텍스트를 바꾸지 않고 db에 바로 꽂음 -> 불일치 발생
    public void bulkUpdate(){

        //member1, member2 가 비회원으로 이름이 변경되어야 함.
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        List<Member> result = queryFactory.selectFrom(member).fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }
    //벌크 연산 이후 em.flush(), em.clear()를 통해 영속성 컨텍스트를 초기화 해줌

    //jpa는 select query로 db의 값을 가져와도, 영속성 컨텍스트의 값을 우선으로 가짐.

    @Test
    public void bulkAdd(){
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

    }

    @Test
    public void bulkDelete(){
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();
    }

    @Test
    public void sqlFunction(){
        List<String> result = queryFactory.select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2(){
        List<String> result = queryFactory.select(member.username)
                .from(member)
                .where(member.username.eq(
                       member.username.lower()
                )).fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

}

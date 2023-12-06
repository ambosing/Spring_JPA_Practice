package study.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory query;

    @BeforeEach
    void before() {
        query = new JPAQueryFactory(em);
        //given
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
    @DisplayName("startQuerydsl")
    void startQuerydsl() throws Exception {
        //given
        //when
        Member findMember = query
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) // 파라미터 바인딩 처리
                .fetchOne();
        //then
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("search")
    void search() throws Exception {
        //given
        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.between(10, 30)))
                .fetchOne();
        //when

        //then
        assertThat(findMember).isNotNull();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("searchAndParam")
    void searchAndParam() throws Exception {
        //given
        Member findMember = query
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();
        //when

        //then
        assertThat(findMember).isNotNull();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    @DisplayName("resultFetchTest")
    void resultFetchTest() throws Exception {
        //given

        //when
        List<Member> fetch = query
                .selectFrom(member)
                .fetch();

        Member fetchOne = query
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = query
                .selectFrom(member)
                .fetchFirst();

        QueryResults<Member> results = query
                .selectFrom(member)
                .fetchResults(); // deprecated

//        results.getTotal();
//        List<Member> content = results.getResults();


        long total = query
                .selectFrom(member)
                .fetchCount(); // deprecated

        //then

    }

    /**
     * 회원 정렬 순서
     * 1. 나이 내림차순
     * 2. 이름 올림차순
     * 단, 2에서 이름이 없으면 마지막에 출력
     */
    @Test
    @DisplayName("sort")
    void sort() throws Exception {
        //given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        //when
        List<Member> result = query
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        //then
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    @DisplayName("paging1")
    void paging1() throws Exception {
        //given

        //when
        List<Member> results = query
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        //then
        assertThat(results.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("paging2")
    void paging2() throws Exception {
        //given

        //when
        QueryResults<Member> queryResults = query
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        //then
        assertThat(queryResults.getTotal()).isEqualTo(4);
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    @DisplayName("join")
    void join() throws Exception {
        //given

        //when
        List<Member> result = query
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타 조인
     * : 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    @DisplayName("thetaJoin")
    void thetaJoin() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        //when
        List<Member> result = query
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        //then
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    @DisplayName("joinOnFiltering")
    void joinOnFiltering() throws Exception {
        //given

        //when
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
//                .where(team.name.eq("teamA"))
                .fetch();
        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }


    /**
     * 연관관계가 없는 엔티티 외부 조인
     * : 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    @DisplayName("joinOnNoRelation")
    void joinOnNoRelation() throws Exception {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        //when
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(member.team)
//                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    @DisplayName("fetchJoinNo")
    void fetchJoinNo() throws Exception {
        //given
        em.flush();
        em.clear();
        //when
        Member findMember = query
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //then
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();

    }

    @Test
    @DisplayName("fetchJoinUse")
    void fetchJoinUse() throws Exception {
        //given
        em.flush();
        em.clear();
        //when
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .join(member.team, team).fetchJoin()
                .fetch();

        //then
        for (Tuple member1 : result) {
            System.out.println("member1 = " + member1);
        }
//
//        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.get(0).getTeam());
//        assertThat(loaded).as("페치 조인 미적용").isTrue();

    }

    @Test
    @DisplayName("innerJoinUse")
    void innerJoinUse() throws Exception {
        //given
        em.flush();
        em.clear();
        //when
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .join(member.team, team)
                .fetch();

        //then
        for (Tuple member1 : result) {
            System.out.println("member1 = " + member1);
        }

    }

    /**
     * 나이가 가장 많은 회원을 조회
     */
    @Test
    @DisplayName("subQuery")
    void subQuery() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Member> result = query
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions.select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        //then

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원을 조회
     */
    @Test
    @DisplayName("subQueryGoe")
    void subQueryGoe() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Member> result = query
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions.select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        //then

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 나이가 10살 초과인 사람 구하기
     */
    @Test
    @DisplayName("subQueryIn")
    void subQueryIn() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Member> result = query
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions.select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
        //then

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    /**
     * select sub query
     */
    @Test
    @DisplayName("selectSubQuery")
    void selectSubQuery() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<Tuple> result = query
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();
        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    @DisplayName("basicCase")
    void basicCase() throws Exception {
        //given

        //when
        List<String> result = query
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("complexCase")
    void complexCase() throws Exception {
        //given

        //when
        List<String> result = query
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0 ~ 20살")
                        .when(member.age.between(21, 30)).then("21 ~ 30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("constant")
    void constant() throws Exception {
        //given

        //when
        List<Tuple> result = query
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        //then
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @Test
    @DisplayName("concat")
    void concat() throws Exception {
        //given

        //when
        List<String> result = query
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("simpleProjection")
    void simpleProjection() throws Exception {
        //given

        //when
        List<String> result = query
                .select(member.username)
                .from(member)
                .fetch();

        //then
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("tupleProjection")
    void tupleProjection() throws Exception {
        //given

        //when
        List<Tuple> result = query
                .select(member.username, member.age)
                .from(member)
                .fetch();
        //then
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);

        }
    }

    @Test
    @DisplayName("findDtoByJPQL")
    void findDtoByJpql() throws Exception {
        //given

        //when
        String qlString = "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m";
        List<MemberDto> resultList = em.createQuery(qlString, MemberDto.class).getResultList();
        //then
        for (MemberDto memberDto : resultList) {
            System.out.println(memberDto);
        }

    }

    @Test
    @DisplayName("findDtoBySetter")
    void findDtoBySetter() throws Exception {
        //given

        //when
        List<MemberDto> result = query
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch(); // 기본 생성자가 있어야 하고, Setter로 설정
        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("findDtoByField")
    void findDtoByField() throws Exception {
        //given

        //when
        List<MemberDto> result = query
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch(); // 기본 생성자 없어도 됨
        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("findDtoByConstructor")
    void findDtoByConstructor() throws Exception {
        //given

        //when
        List<MemberDto> result = query
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch(); // 생성자로 Projection
        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("findUserDtoAs")
    void findUserDtoAs() throws Exception {
        //given
        QMember memberSub = new QMember("memberSub");

        //when
        List<UserDto> result = query
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"), // as로 필드명 변경

                        Expressions.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age"))) // SubQuery as로 필드명 변경
                .from(member)
                .fetch();
        //then
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    @DisplayName("findUserDtoByConstructor")
    void findUserDtoByConstructor() throws Exception {
        //given

        //when
        List<UserDto> result = query
                .select(Projections.constructor(UserDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch(); // 생성자로 Projection
        //then
        for (UserDto userDto : result) {
            System.out.println("memberDto = " + userDto);
        }
    }

    @Test
    @DisplayName("findDtoByQueryProjection")
    void findDtoByQueryProjection() throws Exception {
        //given

        //when
        List<MemberDto> result = query
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        //then
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    @DisplayName("dynamicQuery_BooleanBuilder")
    void dynamicQueryBooleanBuilder() throws Exception {
        //given
        String usernameParam = "member1";
        Integer ageParam = 10;
        //when
        List<Member> result = searchMember1(usernameParam, ageParam);
        //then
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return query
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    @DisplayName("dynamicQuery_WhereParam")
    void dynamicQueryWhereParam() throws Exception {
        //given
        String usernameParam = "member1";
        Integer ageParam = 10;
        //when
        List<Member> result = searchMember2(usernameParam, ageParam);
        //then
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return query
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    @DisplayName("bulkUpdate")
    void bulkUpdate() throws Exception {
        //given

        //when

        //member1 = 10 -> member1
        //member2 = 20 -> member2
        //member3 = 30 -> member3
        //member4 = 40 -> member4
        long count = query
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();


        //member1 = 10 -> 비회원
        //member2 = 20 -> 비회원
        //member3 = 30 -> member3
        //member4 = 40 -> member4

        List<Member> result = query
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }

        //then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("bulkAdd")
    void bulkAdd() throws Exception {
        //given

        //when
        long count = query
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
        //then
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("bulkDelete")
    void bulkDelete() throws Exception {
        //given

        //when
        long count = query
                .delete(member)
                .where(member.age.gt(18))
                .execute();
        //then
        assertThat(count).isEqualTo(3);
    }

    @Test
    public void sqlFunction() {
        List<String> result = query
                .select(
                        Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                                member.username, "member",
                                "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    @DisplayName("sqlFunction2")
    void sqlFunction2() throws Exception {
        //given

        //when
        List<String> result = query
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})",
//                                member.username
//                        )))
                .where(member.username.eq(member.username.lower())) // ANSI 표준 함수들은 querydsl에서 내장하고 있다.
                .fetch();
        //then

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }
}

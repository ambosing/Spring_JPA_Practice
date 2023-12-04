package study.datajpa.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import study.datajpa.dto.MemberDto;
import study.datajpa.entity.Member;
import study.datajpa.entity.Team;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback(value = false)
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    TeamRepository teamRepository;

    @PersistenceContext
    EntityManager em;

    @Test
    @DisplayName("testMember")
    void testMember() throws Exception {
        //given
        Member member = new Member("memberA");
        //when
        Member savedMember = memberRepository.save(member);
        Member findMember = memberRepository.findById(savedMember.getId()).get();
        //then
        assertThat(findMember.getId()).isEqualTo(savedMember.getId());
        assertThat(findMember.getUsername()).isEqualTo(savedMember.getUsername());
        assertThat(findMember).isEqualTo(savedMember);
    }


    @Test
    @DisplayName("basicCRUD")
    void basicCrud() throws Exception {
        //given
        Member member1 = new Member("member1");
        Member member2 = new Member("member2");
        memberRepository.save(member1);
        memberRepository.save(member2);

        //when
        Member findMember1 = memberRepository.findById(member1.getId()).get();
        Member findMember2 = memberRepository.findById(member2.getId()).get();

        //then
        assertThat(findMember1).isEqualTo(member1);
        assertThat(findMember2).isEqualTo(member2);

        // 리스트 검증 조회
        List<Member> all = memberRepository.findAll();
        assertThat(all.size()).isEqualTo(2);

        //카운트 검증
        long count = memberRepository.count();
        assertThat(count).isEqualTo(2);

        //삭제 검증
        memberRepository.delete(member1);
        long deletedCount = memberRepository.count();
        assertThat(deletedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("findByUsernameAngAgeGreaterThen")
    void findByUsernameAngAgeGreaterThen() throws Exception {
        //given
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("AAA", 20);

        memberRepository.save(m1);
        memberRepository.save(m2);
        //when
        List<Member> result = memberRepository.findByUsernameAndAgeGreaterThan("AAA", 15);

        //then
        assertThat(result.get(0).getUsername()).isEqualTo("AAA");
        assertThat(result.get(0).getAge()).isEqualTo(20);
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("testNamedQuery")
    void testNamedQuery() throws Exception {
        //given
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);

        memberRepository.save(m1);
        memberRepository.save(m2);
        //when
        List<Member> result = memberRepository.findByUsername("AAA");
        Member findMember = result.get(0);

        //then
        assertThat(findMember).isEqualTo(m1);
    }

    @Test
    @DisplayName("testNamedQuery")
    void testQuery() throws Exception {
        //given
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);

        memberRepository.save(m1);
        memberRepository.save(m2);
        //when
        List<Member> result = memberRepository.findUser("AAA", 10);
        Member findMember = result.get(0);

        //then
        assertThat(findMember).isEqualTo(m1);
    }

    @Test
    @DisplayName("findUsernameList")
    void findUsernameList() throws Exception {
        //given
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 20);

        memberRepository.save(m1);
        memberRepository.save(m2);
        //when
        List<String> result = memberRepository.findUseranmeList();

        //then
        assertThat(result.get(0)).isEqualTo("AAA");
    }


    @Test
    @DisplayName("findUsernameList")
    void findMemberDto() throws Exception {
        //given
        Member m1 = new Member("AAA", 10);
        Team team = new Team("teamA");
        teamRepository.save(team);
        m1.setTeam(team);
        memberRepository.save(m1);
        //when
        List<MemberDto> memberDto = memberRepository.findMemberDto();


        //then
        for (MemberDto dto : memberDto) {
            System.out.println("dto = " + dto);
        }
    }

    @Test
    @DisplayName("findNames")
    void findNames() throws Exception {
        //given
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 10);
        memberRepository.save(m1);
        memberRepository.save(m2);
        //when
        List<Member> results = memberRepository.findByNames(List.of(new String[]{"AAA", "BBB"}));
        //then
        for (Member result : results) {
            System.out.println("result = " + result);
        }
    }


    @Test
    @DisplayName("findNames")
    void returnType() throws Exception {
        //given
        Member m1 = new Member("AAA", 10);
        Member m2 = new Member("BBB", 10);
        memberRepository.save(m1);
        memberRepository.save(m2);
        //when
        Optional<Member> result = memberRepository.findOptionalByUsername("AAA");
        //then
        System.out.println(result);
    }

    @Test
    @DisplayName("paging")
    void paging() throws Exception {
        //given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 10));
        memberRepository.save(new Member("member3", 10));
        memberRepository.save(new Member("member4", 10));
        memberRepository.save(new Member("member5", 10));
        memberRepository.save(new Member("member6", 10));
        memberRepository.save(new Member("member7", 10));

        int age = 10;
        int offset = 1;
        int limit = 3;
        PageRequest pageRequest = PageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "username"));

        //when
        Page<Member> members = memberRepository.findByAge(age, pageRequest);
        //then
        List<Member> content = members.getContent();
        long totalElements = members.getTotalElements();

        assertThat(content.size()).isEqualTo(3);
        assertThat(members.getTotalElements()).isEqualTo(7);
        assertThat(members.getNumber()).isEqualTo(1);
        assertThat(members.getTotalPages()).isEqualTo(3);
        assertThat(members.isFirst()).isFalse();
        assertThat(members.hasNext()).isTrue();
    }

    @Test
    @DisplayName("slice")
    void slice() throws Exception {
        //given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 10));
        memberRepository.save(new Member("member3", 10));
        memberRepository.save(new Member("member4", 10));
        memberRepository.save(new Member("member5", 10));
        memberRepository.save(new Member("member6", 10));
        memberRepository.save(new Member("member7", 10));

        int age = 10;
        int offset = 1;
        int limit = 3;
        PageRequest pageRequest = PageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "username"));

        //when
        Slice<Member> members = memberRepository.findSliceByAge(age, pageRequest);
        //then
        List<Member> content = members.getContent();

        assertThat(content.size()).isEqualTo(3);
        assertThat(members.getNumber()).isEqualTo(1);
        assertThat(members.isFirst()).isFalse();
        assertThat(members.hasNext()).isTrue();
    }

    @Test
    @DisplayName("countQueryTest")
    void countQueryTest() throws Exception {
        //given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 10));
        memberRepository.save(new Member("member3", 10));
        memberRepository.save(new Member("member4", 10));
        memberRepository.save(new Member("member5", 10));
        memberRepository.save(new Member("member6", 10));
        memberRepository.save(new Member("member7", 10));

        int age = 10;
        int offset = 1;
        int limit = 3;
        PageRequest pageRequest = PageRequest.of(offset, limit, Sort.by(Sort.Direction.DESC, "username"));

        //when
        Slice<Member> members = memberRepository.findQueryByAge(age, pageRequest);
        Slice<MemberDto> toMap = members.map(member -> new MemberDto(member.getId(), member.getUsername(), null));
        for (MemberDto memberDto : toMap) {
            System.out.println("memberDto = " + memberDto);
        }
        //then
        List<Member> content = members.getContent();

        assertThat(content.size()).isEqualTo(3);
        assertThat(members.getNumber()).isEqualTo(1);
        assertThat(members.isFirst()).isFalse();
        assertThat(members.hasNext()).isTrue();
    }


    @Test
    @DisplayName("bulkUpdate")
    void bulkUpdate() throws Exception {
        //given
        memberRepository.save(new Member("member1", 10));
        memberRepository.save(new Member("member2", 19));
        memberRepository.save(new Member("member3", 20));
        memberRepository.save(new Member("member4", 21));
        memberRepository.save(new Member("member5", 40));
        //when
        int resultCount = memberRepository.bulkAgePlus(20);
        List<Member> members = memberRepository.findByUsername("member5");
        Member member = members.get(0);
        System.out.println("member = " + member);

        //then
        assertThat(resultCount).isEqualTo(3);

    }

    @Test
    @DisplayName("findMemberLazy")
    void findMemberLazy() throws Exception {
        //given
        //member1 -> teamA
        //member2 -> teamB

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        teamRepository.save(teamA);
        teamRepository.save(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 10, teamB);

        memberRepository.save(member1);
        memberRepository.save(member2);

        em.flush();
        em.clear();
        //when
        List<Member> members = memberRepository.findAll();

        for (Member member : members) {
            System.out.println("member.getUsername() = " + member.getUsername());
            System.out.println("member.team = " + member.getTeam().getName());

        }
    }

    @Test
    @DisplayName("fetchJoin")
    void fetchJoin() throws Exception {
        //given
        //member1 -> teamA
        //member2 -> teamB

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        teamRepository.save(teamA);
        teamRepository.save(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 10, teamB);

        memberRepository.save(member1);
        memberRepository.save(member2);

        em.flush();
        em.clear();
        //when
        List<Member> members = memberRepository.findMemberFetchJoin();

        for (Member member : members) {
            System.out.println("member.getUsername() = " + member.getUsername());
            System.out.println("member.team = " + member.getTeam().getName());

        }
    }

    @Test
    @DisplayName("entityGraph")
    void entityGraph() throws Exception {
        //given
        //member1 -> teamA
        //member2 -> teamB

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        teamRepository.save(teamA);
        teamRepository.save(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member1", 10, teamB);

        memberRepository.save(member1);
        memberRepository.save(member2);

        em.flush();
        em.clear();
        //when
        List<Member> members = memberRepository.findEntityGraphByUsername("member1");

        for (Member member : members) {
            System.out.println("member.getUsername() = " + member.getUsername());
            System.out.println("member.team = " + member.getTeam().getName());

        }
    }

    @Test
    @DisplayName("queryHint")
    void queryHint() throws Exception {
        //given
        Member member1 = memberRepository.save(new Member("member1", 10));
        em.flush();
        em.clear();

        //when
        Member findMember = memberRepository.findReadOnlyByUsername(member1.getUsername());
        findMember.setUsername("member2");
        em.flush();
        //then

    }

    @Test
    @DisplayName("lock")
    void lock() throws Exception {
        //given
        Member member1 = memberRepository.save(new Member("member1", 10));
        em.flush();
        em.clear();

        //when
        Member findMember = memberRepository.findLockByUsername(member1.getUsername()).get(0);

        //then
    }

    @Test
    @DisplayName("callCustom")
    void callCustom() throws Exception {
        //given
        List<Member> memberCustom = memberRepository.findMemberCustom();
        //when

        //then

    }

    @Test
    @DisplayName("specBasic")
    void specBasic() throws Exception {
        //given
        Team teamA = new Team("teamA");
        em.persist(teamA);

        Member m1 = new Member("m1", 0, teamA);
        Member m2 = new Member("m2", 0, teamA);

        em.persist(m1);
        em.persist(m2);

        em.flush();
        em.clear();

        memberRepository.findAll();
        //when
        Specification<Member> spec = MemberSpec.username("m1").and(MemberSpec.teamName("teamA"));
//        List<Member> result = memberRepository.findAll(spec);

        //then
//        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("queryByExample")
    void queryByExample() throws Exception {
        //given
        Team teamA = new Team("teamA");
        em.persist(teamA);

        Member m1 = new Member("m1", 0, teamA);
        Member m2 = new Member("m2", 0, teamA);

        em.persist(m1);
        em.persist(m2);

        em.flush();
        em.clear();
        //when
        //Probe
        Member member = new Member("m1");
        Team team = new Team("teamA");
        member.setTeam(team);

        ExampleMatcher matcher = ExampleMatcher.matching()
                .withIgnorePaths("age");

        Example<Member> example = Example.of(member, matcher);

        List<Member> result = memberRepository.findAll(example);
        //then

        assertThat(result.get(0).getUsername()).isEqualTo("m1");
    }

    @Test
    @DisplayName("projections")
    void projections() throws Exception {
        //given
        Team teamA = new Team("teamA");
        em.persist(teamA);

        Member m1 = new Member("m1", 0, teamA);
        Member m2 = new Member("m2", 0, teamA);

        em.persist(m1);
        em.persist(m2);

        em.flush();
        em.clear();

        //when
        List<NestedClosedProjections> result = memberRepository.findProjectionsByUsername("m1", NestedClosedProjections.class);
        for (NestedClosedProjections usernameOnly : result) {
            System.out.println("usernameOnly = " + usernameOnly);
        }
        //then

    }

    @Test
    @DisplayName("nativeQuery")
    void nativeQuery() throws Exception {
        //given
        Team teamA = new Team("teamA");
        em.persist(teamA);

        Member m1 = new Member("m1", 0, teamA);
        Member m2 = new Member("m2", 0, teamA);

        em.persist(m1);
        em.persist(m2);

        em.flush();
        em.clear();
        //when
        Member result = memberRepository.findByNativeQuery("m1");
        System.out.println("result = " + result);

        //then

    }
}
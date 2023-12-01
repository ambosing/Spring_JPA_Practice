package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    
    //select m from Member m where m.name = ? <- JPQL 생성
    List<Member> findByName(String name);
}

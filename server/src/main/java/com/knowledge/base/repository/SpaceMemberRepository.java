package com.knowledge.base.repository;

import com.knowledge.base.entity.Space;
import com.knowledge.base.entity.SpaceMember;
import com.knowledge.base.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpaceMemberRepository extends JpaRepository<SpaceMember, Long> {
    
    Optional<SpaceMember> findBySpaceAndUser(Space space, User user);
    
    List<SpaceMember> findBySpace(Space space);
    
    List<SpaceMember> findByUser(User user);
    
    boolean existsBySpaceAndUser(Space space, User user);
    
    void deleteBySpaceAndUser(Space space, User user);
}

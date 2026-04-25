package com.knowledge.base.repository;

import com.knowledge.base.entity.Space;
import com.knowledge.base.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpaceRepository extends JpaRepository<Space, Long> {
    
    List<Space> findByOwner(User owner);
    
    @Query("SELECT s FROM Space s JOIN s.members m WHERE m = :user")
    List<Space> findByMember(@Param("user") User user);
    
    @Query("SELECT s FROM Space s WHERE s.owner = :user OR :user MEMBER OF s.members")
    List<Space> findByUserAccess(@Param("user") User user);
    
    Optional<Space> findByIdAndOwner(Long id, User owner);
}

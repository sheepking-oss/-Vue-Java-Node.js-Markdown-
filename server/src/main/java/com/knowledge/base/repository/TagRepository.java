package com.knowledge.base.repository;

import com.knowledge.base.entity.Space;
import com.knowledge.base.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    List<Tag> findBySpace(Space space);
    
    Optional<Tag> findByNameAndSpace(String name, Space space);
    
    boolean existsByNameAndSpace(String name, Space space);
}

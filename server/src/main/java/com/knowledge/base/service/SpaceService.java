package com.knowledge.base.service;

import com.knowledge.base.entity.Space;
import com.knowledge.base.entity.SpaceMember;
import com.knowledge.base.entity.User;
import com.knowledge.base.repository.SpaceMemberRepository;
import com.knowledge.base.repository.SpaceRepository;
import com.knowledge.base.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceMemberRepository spaceMemberRepository;

    @Transactional
    public Space createSpace(String name, String description, UserPrincipal currentUser) {
        Space space = new Space();
        space.setName(name);
        space.setDescription(description);
        space.setOwner(currentUser.getUser());

        space = spaceRepository.save(space);

        SpaceMember member = new SpaceMember();
        member.setSpace(space);
        member.setUser(currentUser.getUser());
        member.setRole(SpaceMember.Role.OWNER);
        spaceMemberRepository.save(member);

        return space;
    }

    @Transactional
    public Space updateSpace(Long spaceId, String name, String description, UserPrincipal currentUser) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found"));

        if (!isOwner(space, currentUser.getUser())) {
            throw new SecurityException("No permission to update space");
        }

        if (name != null) {
            space.setName(name);
        }
        if (description != null) {
            space.setDescription(description);
        }

        return spaceRepository.save(space);
    }

    @Transactional
    public void deleteSpace(Long spaceId, UserPrincipal currentUser) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found"));

        if (!isOwner(space, currentUser.getUser())) {
            throw new SecurityException("No permission to delete space");
        }

        spaceRepository.delete(space);
    }

    @Transactional
    public SpaceMember addMember(Long spaceId, Long userId, SpaceMember.Role role, UserPrincipal currentUser) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found"));

        if (!isAdmin(space, currentUser.getUser())) {
            throw new SecurityException("No permission to add members");
        }

        if (spaceMemberRepository.existsBySpaceAndUser(space, new User() {{ setId(userId); }})) {
            throw new IllegalArgumentException("User is already a member");
        }

        SpaceMember member = new SpaceMember();
        member.setSpace(space);
        User user = new User();
        user.setId(userId);
        member.setUser(user);
        member.setRole(role);

        return spaceMemberRepository.save(member);
    }

    @Transactional
    public SpaceMember updateMemberRole(Long spaceId, Long userId, SpaceMember.Role role, UserPrincipal currentUser) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found"));

        if (!isAdmin(space, currentUser.getUser())) {
            throw new SecurityException("No permission to manage members");
        }

        SpaceMember member = spaceMemberRepository.findBySpaceAndUser(space, new User() {{ setId(userId); }})
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getRole() == SpaceMember.Role.OWNER) {
            throw new IllegalArgumentException("Cannot change owner role");
        }

        member.setRole(role);
        return spaceMemberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long spaceId, Long userId, UserPrincipal currentUser) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found"));

        if (!isAdmin(space, currentUser.getUser())) {
            throw new SecurityException("No permission to remove members");
        }

        SpaceMember member = spaceMemberRepository.findBySpaceAndUser(space, new User() {{ setId(userId); }})
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getRole() == SpaceMember.Role.OWNER) {
            throw new IllegalArgumentException("Cannot remove owner");
        }

        spaceMemberRepository.delete(member);
    }

    public Optional<Space> getSpace(Long spaceId) {
        return spaceRepository.findById(spaceId);
    }

    public List<Space> getUserSpaces(UserPrincipal currentUser) {
        return spaceRepository.findByUserAccess(currentUser.getUser());
    }

    public List<SpaceMember> getSpaceMembers(Space space) {
        return spaceMemberRepository.findBySpace(space);
    }

    public Optional<SpaceMember> getMemberRole(Space space, User user) {
        return spaceMemberRepository.findBySpaceAndUser(space, user);
    }

    private boolean isOwner(Space space, User user) {
        return space.getOwner().getId().equals(user.getId());
    }

    private boolean isAdmin(Space space, User user) {
        if (isOwner(space, user)) {
            return true;
        }

        Optional<SpaceMember> member = spaceMemberRepository.findBySpaceAndUser(space, user);
        return member.map(m -> m.getRole() == SpaceMember.Role.ADMIN).orElse(false);
    }
}

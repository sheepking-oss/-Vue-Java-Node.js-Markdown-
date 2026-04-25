package com.knowledge.base.service;

import com.knowledge.base.entity.Space;
import com.knowledge.base.entity.Tag;
import com.knowledge.base.entity.User;
import com.knowledge.base.repository.SpaceMemberRepository;
import com.knowledge.base.repository.TagRepository;
import com.knowledge.base.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final SpaceMemberRepository spaceMemberRepository;

    @Transactional
    public Tag createTag(Space space, String name, String color, UserPrincipal currentUser) {
        if (!hasEditPermission(space, currentUser.getUser())) {
            throw new SecurityException("No permission to create tags");
        }

        if (tagRepository.existsByNameAndSpace(name, space)) {
            throw new IllegalArgumentException("Tag with this name already exists");
        }

        Tag tag = new Tag();
        tag.setName(name);
        tag.setColor(color != null ? color : "#3b82f6");
        tag.setSpace(space);

        return tagRepository.save(tag);
    }

    @Transactional
    public Tag updateTag(Long tagId, String name, String color, UserPrincipal currentUser) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));

        if (!hasEditPermission(tag.getSpace(), currentUser.getUser())) {
            throw new SecurityException("No permission to update tags");
        }

        if (name != null && !name.equals(tag.getName())) {
            if (tagRepository.existsByNameAndSpace(name, tag.getSpace())) {
                throw new IllegalArgumentException("Tag with this name already exists");
            }
            tag.setName(name);
        }

        if (color != null) {
            tag.setColor(color);
        }

        return tagRepository.save(tag);
    }

    @Transactional
    public void deleteTag(Long tagId, UserPrincipal currentUser) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));

        if (!hasEditPermission(tag.getSpace(), currentUser.getUser())) {
            throw new SecurityException("No permission to delete tags");
        }

        tagRepository.delete(tag);
    }

    public List<Tag> getSpaceTags(Space space) {
        return tagRepository.findBySpace(space);
    }

    public Optional<Tag> getTag(Long tagId) {
        return tagRepository.findById(tagId);
    }

    private boolean hasEditPermission(Space space, User user) {
        if (space.getOwner().getId().equals(user.getId())) {
            return true;
        }

        var member = spaceMemberRepository.findBySpaceAndUser(space, user);
        if (member.isEmpty()) {
            return false;
        }

        var role = member.get().getRole();
        return role == com.knowledge.base.entity.SpaceMember.Role.ADMIN 
                || role == com.knowledge.base.entity.SpaceMember.Role.EDITOR;
    }
}

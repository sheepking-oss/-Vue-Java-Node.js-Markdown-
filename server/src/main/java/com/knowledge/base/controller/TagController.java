package com.knowledge.base.controller;

import com.knowledge.base.dto.TagDTO;
import com.knowledge.base.entity.Space;
import com.knowledge.base.entity.Tag;
import com.knowledge.base.security.UserPrincipal;
import com.knowledge.base.service.SpaceService;
import com.knowledge.base.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final SpaceService spaceService;

    @PostMapping
    public ResponseEntity<?> createTag(
            @RequestParam Long spaceId,
            @RequestBody TagDTO.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Space space = spaceService.getSpace(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found"));
        
        Tag tag = tagService.createTag(space, request.getName(), request.getColor(), currentUser);
        return ResponseEntity.ok(convertToDTO(tag));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTag(
            @PathVariable Long id,
            @RequestBody TagDTO.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Tag tag = tagService.updateTag(id, request.getName(), request.getColor(), currentUser);
        return ResponseEntity.ok(convertToDTO(tag));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTag(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        tagService.deleteTag(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/space/{spaceId}")
    public ResponseEntity<?> getSpaceTags(@PathVariable Long spaceId) {
        Space space = spaceService.getSpace(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found"));
        
        List<Tag> tags = tagService.getSpaceTags(space);
        return ResponseEntity.ok(tags.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    private TagDTO.Response convertToDTO(Tag tag) {
        TagDTO.Response dto = new TagDTO.Response();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setColor(tag.getColor());
        dto.setSpaceId(tag.getSpace() != null ? tag.getSpace().getId() : null);
        return dto;
    }
}

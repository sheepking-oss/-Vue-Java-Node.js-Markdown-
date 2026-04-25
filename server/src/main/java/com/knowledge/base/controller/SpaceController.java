package com.knowledge.base.controller;

import com.knowledge.base.dto.SpaceDTO;
import com.knowledge.base.dto.UserDTO;
import com.knowledge.base.entity.Space;
import com.knowledge.base.entity.SpaceMember;
import com.knowledge.base.security.UserPrincipal;
import com.knowledge.base.service.SpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @PostMapping
    public ResponseEntity<?> createSpace(
            @RequestBody SpaceDTO.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Space space = spaceService.createSpace(request.getName(), request.getDescription(), currentUser);
        return ResponseEntity.ok(convertToDTO(space));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSpace(
            @PathVariable Long id,
            @RequestBody SpaceDTO.UpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Space space = spaceService.updateSpace(id, request.getName(), request.getDescription(), currentUser);
        return ResponseEntity.ok(convertToDTO(space));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSpace(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        spaceService.deleteSpace(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSpace(@PathVariable Long id) {
        return spaceService.getSpace(id)
                .map(space -> ResponseEntity.ok(convertToDTO(space)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<?> getUserSpaces(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Space> spaces = spaceService.getUserSpaces(currentUser);
        return ResponseEntity.ok(spaces.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @PostMapping("/{spaceId}/members")
    public ResponseEntity<?> addMember(
            @PathVariable Long spaceId,
            @RequestBody SpaceDTO.AddMemberRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        SpaceMember.Role role = SpaceMember.Role.valueOf(request.getRole().toUpperCase());
        SpaceMember member = spaceService.addMember(spaceId, request.getUserId(), role, currentUser);
        return ResponseEntity.ok(member);
    }

    @PutMapping("/{spaceId}/members/{userId}")
    public ResponseEntity<?> updateMemberRole(
            @PathVariable Long spaceId,
            @PathVariable Long userId,
            @RequestParam String role,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        SpaceMember.Role roleEnum = SpaceMember.Role.valueOf(role.toUpperCase());
        SpaceMember member = spaceService.updateMemberRole(spaceId, userId, roleEnum, currentUser);
        return ResponseEntity.ok(member);
    }

    @DeleteMapping("/{spaceId}/members/{userId}")
    public ResponseEntity<?> removeMember(
            @PathVariable Long spaceId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        spaceService.removeMember(spaceId, userId, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{spaceId}/members")
    public ResponseEntity<?> getMembers(@PathVariable Long spaceId) {
        Space space = spaceService.getSpace(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found"));
        
        List<SpaceMember> members = spaceService.getSpaceMembers(space);
        return ResponseEntity.ok(members);
    }

    private SpaceDTO.Response convertToDTO(Space space) {
        SpaceDTO.Response dto = new SpaceDTO.Response();
        dto.setId(space.getId());
        dto.setName(space.getName());
        dto.setDescription(space.getDescription());
        
        if (space.getOwner() != null) {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(space.getOwner().getId());
            userDTO.setUsername(space.getOwner().getUsername());
            dto.setOwner(userDTO);
        }
        
        dto.setCreatedAt(space.getCreatedAt() != null ? space.getCreatedAt().format(dateTimeFormatter) : null);
        dto.setUpdatedAt(space.getUpdatedAt() != null ? space.getUpdatedAt().format(dateTimeFormatter) : null);
        
        return dto;
    }
}

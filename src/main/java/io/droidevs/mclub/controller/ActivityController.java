package io.droidevs.mclub.controller;

import io.droidevs.mclub.dto.ActivityCreateRequest;
import io.droidevs.mclub.dto.ActivityDto;
import io.droidevs.mclub.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final io.droidevs.mclub.mapper.ActivityCreateRequestMapper activityCreateRequestMapper;

    @PostMapping
    public ResponseEntity<ActivityDto> create(@Valid @RequestBody ActivityCreateRequest request, Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(activityService.create(activityCreateRequestMapper.toDto(request), auth.getName()));
    }

    @GetMapping("/club/{clubId}")
    public ResponseEntity<List<ActivityDto>> getByClub(@PathVariable UUID clubId) {
        return ResponseEntity.ok(activityService.getByClub(clubId));
    }
}

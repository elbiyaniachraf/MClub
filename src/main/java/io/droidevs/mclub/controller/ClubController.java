package io.droidevs.mclub.controller;
import io.droidevs.mclub.dto.ClubDto;
import io.droidevs.mclub.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {
    private final ClubService clubService;

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ClubDto> createClub(@RequestBody ClubDto dto, Authentication auth) {
        return ResponseEntity.ok(clubService.createClub(dto, auth.getName()));
    }

    @GetMapping
    public ResponseEntity<Page<ClubDto>> getClubs(Pageable pageable) {
        return ResponseEntity.ok(clubService.getAllClubs(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClubDto> getClub(@PathVariable UUID id) {
        return ResponseEntity.ok(clubService.getClub(id));
    }
}

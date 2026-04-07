package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.ClubApplicationDto;
import io.droidevs.mclub.mapper.ClubApplicationMapper;
import io.droidevs.mclub.repository.*;
import io.droidevs.mclub.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClubApplicationService {

    private final ClubApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final ClubApplicationMapper mapper;

    public ClubApplicationDto submitApplication(ClubApplicationDto dto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ClubApplication app = ClubApplication.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .submittedBy(user)
                .status("PENDING")
                .build();

        return mapper.toDto(applicationRepository.save(app));
    }

    public List<ClubApplicationDto> getPendingApplications() {
        return applicationRepository.findByStatus("PENDING").stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveApplication(UUID id) {
        ClubApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!"PENDING".equals(app.getStatus())) {
            throw new RuntimeException("Application is already processed");
        }

        app.setStatus("APPROVED");
        applicationRepository.save(app);

        // Create the club based on the application
        Club club = Club.builder()
                .name(app.getName())
                .description(app.getDescription())
                .createdBy(app.getSubmittedBy())
                .build();
        club = clubRepository.save(club);

        // Assign the user who submitted the application as the Club Admin
        Membership membership = Membership.builder()
                .user(app.getSubmittedBy())
                .club(club)
                .role(Role.CLUB_ADMIN)
                .status("APPROVED")
                .build();
        membershipRepository.save(membership);
    }

    @Transactional
    public void rejectApplication(UUID id) {
        ClubApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        app.setStatus("REJECTED");
        applicationRepository.save(app);
    }
}


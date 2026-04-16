package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.ClubApplicationDto;
import io.droidevs.mclub.mapper.ClubApplicationEntityMapper;
import io.droidevs.mclub.mapper.ClubApplicationMapper;
import io.droidevs.mclub.mapper.ClubFromApplicationMapper;
import io.droidevs.mclub.mapper.MembershipFactoryMapper;
import io.droidevs.mclub.repository.*;
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
    private final ClubApplicationEntityMapper clubApplicationEntityMapper;
    private final ClubFromApplicationMapper clubFromApplicationMapper;
    private final MembershipFactoryMapper membershipFactoryMapper;

    public ClubApplicationDto submitApplication(ClubApplicationDto dto, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ClubApplication app = clubApplicationEntityMapper.toEntity(dto);
        app.setSubmittedBy(user);
        app.setStatus("PENDING");

        return mapper.toDto(applicationRepository.save(app));
    }

    public List<ClubApplicationDto> getPendingApplications() {
        return applicationRepository.findByStatusWithSubmittedBy("PENDING").stream()
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

        User submitter = app.getSubmittedBy();

        // Create the club based on the application
        Club club = clubFromApplicationMapper.toEntity(app);
        club.setCreatedBy(submitter);
        club = clubRepository.save(club);

        // Assign the user who submitted the application as the Club ADMIN (club-scoped)
        Membership membership = membershipFactoryMapper.create(ClubRole.ADMIN, MembershipStatus.APPROVED);
        membership.setUser(submitter);
        membership.setClub(club);
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

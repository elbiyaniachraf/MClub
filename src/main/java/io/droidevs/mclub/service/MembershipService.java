package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.*;
import io.droidevs.mclub.dto.*;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.*;
import io.droidevs.mclub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MembershipService {
    private final MembershipRepository membershipRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final MembershipMapper membershipMapper;
    private final MembershipFactoryMapper membershipFactoryMapper;

    public MembershipDto joinClub(UUID clubId, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Club club = clubRepository.findById(clubId).orElseThrow(() -> new ResourceNotFoundException("Club not found"));
        if (membershipRepository.findByUserIdAndClubId(user.getId(), clubId).isPresent()) {
            throw new RuntimeException("Already joined");
        }

        Membership m = membershipFactoryMapper.create(ClubRole.MEMBER, MembershipStatus.PENDING);
        m.setUser(user);
        m.setClub(club);

        return membershipMapper.toDto(membershipRepository.save(m));
    }

    public MembershipDto updateStatus(UUID membershipId, String status) {
        Membership m = membershipRepository.findById(membershipId).orElseThrow();
        m.setStatus(MembershipStatus.valueOf(status.toUpperCase()));
        return membershipMapper.toDto(membershipRepository.save(m));
    }

    public MembershipDto updateRole(UUID membershipId, String role) {
        Membership m = membershipRepository.findById(membershipId).orElseThrow();
        m.setRole(ClubRole.valueOf(role.toUpperCase()));
        return membershipMapper.toDto(membershipRepository.save(m));
    }

    public List<MembershipDto> getMembers(UUID clubId) {
        return membershipRepository.findByClubId(clubId).stream().map(membershipMapper::toDto).collect(Collectors.toList());
    }

    public List<MembershipDto> getApprovedMembers(UUID clubId) {
        return membershipRepository.findByClubId(clubId).stream()
                .filter(m -> m.getStatus() == MembershipStatus.APPROVED)
                .map(membershipMapper::toDto)
                .collect(Collectors.toList());
    }
}

package io.droidevs.mclub.service;

import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.dto.ClubDto;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.mapper.ClubEntityMapper;
import io.droidevs.mclub.mapper.ClubMapper;
import io.droidevs.mclub.repository.ClubRepository;
import io.droidevs.mclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClubService {
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ClubMapper clubMapper;
    private final ClubEntityMapper clubEntityMapper;

    public ClubDto createClub(ClubDto dto, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        Club c = clubEntityMapper.toEntity(dto);
        c.setCreatedBy(user);
        return clubMapper.toDto(clubRepository.save(c));
    }

    public Page<ClubDto> getAllClubs(Pageable pageable) {
        return clubRepository.findAll(pageable).map(clubMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ClubDto getClub(UUID id) {
        return clubMapper.toDto(
                clubRepository.findByIdEager(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Club not found"))
        );
    }
}

package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.domain.Membership;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.repository.ClubRepository;
import io.droidevs.mclub.repository.MembershipRepository;
import io.droidevs.mclub.repository.UserRepository;
import io.droidevs.mclub.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/club-admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('CLUB_ADMIN')")
public class WebClubAdminController {

    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    @GetMapping("/clubs")
    public String myManagedClubs(Model model, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        // find clubs where this user is CLUB_ADMIN
        List<Membership> memberships = membershipRepository.findAll().stream()
                .filter(m -> m.getUser().getId().equals(user.getId()) && m.getRole() == Role.CLUB_ADMIN)
                .collect(Collectors.toList());
        List<Club> managedClubs = memberships.stream().map(Membership::getClub).collect(Collectors.toList());
        model.addAttribute("clubs", managedClubs);
        return "my-managed-clubs";
    }

    @GetMapping("/clubs/{clubId}/members")
    public String manageMembers(@PathVariable UUID clubId, Model model, Authentication auth) {
        // Find club
        Club club = clubRepository.findById(clubId).orElseThrow();

        // Ensure current user is admin of this club
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        boolean isAdmin = membershipRepository.findByClubId(clubId).stream()
                .anyMatch(m -> m.getUser().getId().equals(user.getId()) && m.getRole() == Role.CLUB_ADMIN);

        if (!isAdmin) {
            return "redirect:/dashboard";
        }

        List<Membership> members = membershipRepository.findByClubId(clubId);

        model.addAttribute("club", club);
        model.addAttribute("members", members);

        return "manage-members";
    }

    @PostMapping("/memberships/{membershipId}/status")
    public String updateMembershipStatus(@PathVariable UUID membershipId,
                                         @RequestParam String status,
                                         Authentication auth,
                                         RedirectAttributes redirectAttributes) {
        Membership membership = membershipRepository.findById(membershipId).orElseThrow();
        UUID clubId = membership.getClub().getId();

        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        boolean isAdmin = membershipRepository.findByClubId(clubId).stream()
                .anyMatch(m -> m.getUser().getId().equals(user.getId()) && m.getRole() == Role.CLUB_ADMIN);

        if (!isAdmin) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to manage this club.");
            return "redirect:/dashboard";
        }

        membership.setStatus(status);
        membershipRepository.save(membership);
        redirectAttributes.addFlashAttribute("message", "Member status updated to " + status + ".");
        return "redirect:/club-admin/clubs/" + clubId + "/members";
    }

    @PostMapping("/memberships/{membershipId}/kick")
    public String kickMember(@PathVariable UUID membershipId,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        Membership membership = membershipRepository.findById(membershipId).orElseThrow();
        UUID clubId = membership.getClub().getId();

        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        boolean isAdmin = membershipRepository.findByClubId(clubId).stream()
                .anyMatch(m -> m.getUser().getId().equals(user.getId()) && m.getRole() == Role.CLUB_ADMIN);

        if (!isAdmin) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to manage this club.");
            return "redirect:/dashboard";
        }

        membershipRepository.delete(membership);
        redirectAttributes.addFlashAttribute("message", "Member removed from the club.");
        return "redirect:/club-admin/clubs/" + clubId + "/members";
    }
}


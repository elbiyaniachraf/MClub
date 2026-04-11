package io.droidevs.mclub.controller;

import io.droidevs.mclub.domain.Club;
import io.droidevs.mclub.domain.ClubRole;
import io.droidevs.mclub.domain.Membership;
import io.droidevs.mclub.domain.MembershipStatus;
import io.droidevs.mclub.domain.User;
import io.droidevs.mclub.repository.ClubRepository;
import io.droidevs.mclub.repository.MembershipRepository;
import io.droidevs.mclub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
public class WebClubAdminController {

    private final ClubRepository clubRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    private boolean canManageClub(UUID clubId, UUID userId) {
        return membershipRepository.findByClubId(clubId).stream()
                .anyMatch(m -> m.getUser().getId().equals(userId)
                        && (m.getRole() == ClubRole.ADMIN || m.getRole() == ClubRole.STAFF));
    }

    @GetMapping("/clubs")
    public String myManagedClubs(Model model, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        // find clubs where this user is ADMIN/STAFF
        List<Membership> memberships = membershipRepository.findAll().stream()
                .filter(m -> m.getUser().getId().equals(user.getId())
                        && (m.getRole() == ClubRole.ADMIN || m.getRole() == ClubRole.STAFF))
                .collect(Collectors.toList());

        List<Club> managedClubs = memberships.stream().map(Membership::getClub).collect(Collectors.toList());
        model.addAttribute("clubs", managedClubs);
        return "my-managed-clubs";
    }

    @GetMapping("/clubs/{clubId}/memberships")
    public String membershipApplications(@PathVariable UUID clubId, Model model, Authentication auth) {
        Club club = clubRepository.findById(clubId).orElseThrow();

        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        if (!canManageClub(clubId, user.getId())) {
            return "redirect:/dashboard";
        }

        List<Membership> pending = membershipRepository.findByClubIdAndStatus(clubId, MembershipStatus.PENDING);
        model.addAttribute("club", club);
        model.addAttribute("applications", pending);
        return "membership-applications";
    }

    @GetMapping("/clubs/{clubId}/members")
    public String manageMembers(@PathVariable UUID clubId, Model model, Authentication auth) {
        Club club = clubRepository.findById(clubId).orElseThrow();

        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        if (!canManageClub(clubId, user.getId())) {
            return "redirect:/dashboard";
        }

        List<Membership> members = membershipRepository.findByClubIdAndStatus(clubId, MembershipStatus.APPROVED);

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
        if (!canManageClub(clubId, user.getId())) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to manage this club.");
            return "redirect:/dashboard";
        }

        membership.setStatus(io.droidevs.mclub.domain.MembershipStatus.valueOf(status.toUpperCase()));
        membershipRepository.save(membership);
        redirectAttributes.addFlashAttribute("message", "Member status updated to " + status + ".");
        return "redirect:/club-admin/clubs/" + clubId + "/memberships";
    }

    @PostMapping("/memberships/{membershipId}/kick")
    public String kickMember(@PathVariable UUID membershipId,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        Membership membership = membershipRepository.findById(membershipId).orElseThrow();
        UUID clubId = membership.getClub().getId();

        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        if (!canManageClub(clubId, user.getId())) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to manage this club.");
            return "redirect:/dashboard";
        }

        membershipRepository.delete(membership);
        redirectAttributes.addFlashAttribute("message", "Member removed from the club.");
        return "redirect:/club-admin/clubs/" + clubId + "/members";
    }
}

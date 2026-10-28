package br.finedu.user;

import br.finedu.common.PageResponse;
import br.finedu.security.AuthUser;
import br.finedu.security.SessionService;
import br.finedu.user.UserDtos.ActivityView;
import br.finedu.user.UserDtos.ChangeEmailRequest;
import br.finedu.user.UserDtos.ChangePasswordRequest;
import br.finedu.user.UserDtos.DeleteAccountRequest;
import br.finedu.user.UserDtos.MeResponse;
import br.finedu.user.UserDtos.PreferencesRequest;
import br.finedu.user.UserDtos.PreferencesResponse;
import br.finedu.user.UserDtos.UpdateProfileRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class ProfileController {

    private final UserService userService;
    private final SessionService sessions;

    public ProfileController(UserService userService, SessionService sessions) {
        this.userService = userService;
        this.sessions = sessions;
    }

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal AuthUser user) {
        return userService.me(user.id());
    }

    @PutMapping
    public MeResponse update(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody UpdateProfileRequest req) {
        return userService.updateProfile(user.id(), req.displayName());
    }

    @PutMapping("/email")
    public MeResponse changeEmail(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ChangeEmailRequest req) {
        return userService.changeEmail(user.id(), req.email(), req.currentPassword());
    }

    /** Após a troca, as demais sessões são invalidadas e a sessão atual é renovada. */
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthUser user,
                                               @Valid @RequestBody ChangePasswordRequest req,
                                               HttpServletRequest request, HttpServletResponse response) {
        User updated = userService.changePassword(user.id(), req.currentPassword(), req.newPassword());
        sessions.establish(updated, request, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public PreferencesResponse preferences(@AuthenticationPrincipal AuthUser user) {
        return userService.me(user.id()).preferences();
    }

    @PutMapping("/preferences")
    public PreferencesResponse updatePreferences(@AuthenticationPrincipal AuthUser user,
                                                 @Valid @RequestBody PreferencesRequest req) {
        return userService.updatePreferences(user.id(), req);
    }

    @GetMapping("/activity")
    public PageResponse<ActivityView> activity(@AuthenticationPrincipal AuthUser user,
                                               @RequestParam(defaultValue = "0") @Min(0) int page,
                                               @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return userService.activity(user.id(), page, size);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal AuthUser user,
                                              @Valid @RequestBody DeleteAccountRequest req, HttpServletRequest request) {
        userService.deleteAccount(user.id(), req.password());
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }
}

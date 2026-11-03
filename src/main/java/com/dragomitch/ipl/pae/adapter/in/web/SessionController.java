package com.dragomitch.ipl.pae.adapter.in.web;

import com.dragomitch.ipl.pae.adapter.in.web.security.CurrentUser;
import com.dragomitch.ipl.pae.adapter.in.web.security.SessionCookieService;
import com.dragomitch.ipl.pae.domain.dto.UserDto;
import com.dragomitch.ipl.pae.domain.port.in.SessionService;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sign-in, current user and sign-out. The session is a signed JWT in the {@code session} cookie.
 */
@Tag(name = "Session", description = "Sign-in, the signed-in user, sign-out (the session cookie).")
@RestController
@RequestMapping(ApiPaths.BASE + "/session")
public class SessionController {

  /**
   * Credentials of a sign-in.
   *
   * @param username the username
   * @param password the password
   */
  public record SignInRequest(@NotBlank String username, @NotBlank String password) {}

  private final SessionService sessionService;
  private final SessionCookieService sessionCookieService;

  public SessionController(
      SessionService sessionService, SessionCookieService sessionCookieService) {
    this.sessionService = sessionService;
    this.sessionCookieService = sessionCookieService;
  }

  /** Public. Checks the credentials and opens a session (sets the {@code session} cookie). */
  @ApiResponse(
      responseCode = "200",
      description =
          "Signed in: the response sets the session "
              + "cookie (HttpOnly, SameSite=Lax) holding the session JWT",
      headers = @Header(name = HttpHeaders.SET_COOKIE, description = "session=<JWT>"))
  @ApiResponse(
      responseCode = "401",
      description = "Unknown username or wrong password (INVALID_CREDENTIALS)")
  @PostMapping
  public ResponseEntity<UserDto> signin(@RequestBody @Valid SignInRequest credentials) {
    UserDto user = sessionService.signin(credentials.username(), credentials.password());
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, sessionCookieService.sessionCookie(user).toString())
        .body(user);
  }

  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public UserDto showAuthenticatedUser(CurrentUser currentUser) {
    return sessionService.showAuthenticatedUser(currentUser.id());
  }

  /** Closes the session: the stateless JWT cannot be revoked, the cookie is removed. */
  @DeleteMapping
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public ResponseEntity<Void> signout() {
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, sessionCookieService.expiredCookie().toString())
        .build();
  }
}

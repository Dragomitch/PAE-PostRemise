package com.dragomitch.ipl.pae.web;

import com.dragomitch.ipl.pae.business.dto.UserDto;
import com.dragomitch.ipl.pae.security.CurrentUser;
import com.dragomitch.ipl.pae.security.SessionCookieService;
import com.dragomitch.ipl.pae.uccontrollers.SessionUcc;

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
@RestController
@RequestMapping(ApiPaths.BASE + "/session")
public class SessionController {

  /**
   * Credentials of a sign-in.
   *
   * @param username the username
   * @param password the password
   */
  public record SignInRequest(String username, String password) {
  }

  private final SessionUcc sessionUcc;
  private final SessionCookieService sessionCookieService;

  public SessionController(SessionUcc sessionUcc, SessionCookieService sessionCookieService) {
    this.sessionUcc = sessionUcc;
    this.sessionCookieService = sessionCookieService;
  }

  /** Public. Checks the credentials and opens a session (sets the {@code session} cookie). */
  @PostMapping
  public ResponseEntity<UserDto> signin(@RequestBody SignInRequest credentials) {
    UserDto user = sessionUcc.signin(credentials.username(), credentials.password());
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, sessionCookieService.sessionCookie(user).toString())
        .body(user);
  }

  @GetMapping
  @PreAuthorize(ApiPaths.PROFESSOR_OR_STUDENT)
  public UserDto showAuthenticatedUser(CurrentUser currentUser) {
    return sessionUcc.showAuthenticatedUser(currentUser.id());
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

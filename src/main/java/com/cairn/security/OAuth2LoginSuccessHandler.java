package com.cairn.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * WHY: Intercepts successful GitHub logins. Creates or updates the User in the DB, generates a JWT,
 * and redirects the client with the token so it can be used for API requests.
 */
@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final UserRepository userRepository;
  private final JwtService jwtService;

  public OAuth2LoginSuccessHandler(UserRepository userRepository, JwtService jwtService) {
    this.userRepository = userRepository;
    this.jwtService = jwtService;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {

    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    String githubId = oAuth2User.getName(); // For GitHub, this is typically the numeric ID
    String email = oAuth2User.getAttribute("email");
    String login = oAuth2User.getAttribute("login"); // GitHub username

    Optional<User> optionalUser = userRepository.findByGithubId(githubId);
    User user;

    if (optionalUser.isPresent()) {
      user = optionalUser.get();
      // Update email/username if changed
      user.setEmail(email);
      user.setUsername(login);
      userRepository.save(user);
    } else {
      user = new User();
      user.setGithubId(githubId);
      user.setEmail(email);
      user.setUsername(login);
      // First user could be made an ADMIN, but let's stick to default USER.
      // Admins can be set via DB manually for now.
      userRepository.save(user);
    }

    // Generate JWT token
    String token = jwtService.generateToken(user);

    // Redirect to frontend with token in query param
    // In a real app, you might redirect to a specific frontend URL configured via properties.
    String targetUrl = "/?token=" + token;
    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}

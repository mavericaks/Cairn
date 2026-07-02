package com.cairn.security;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * WHY: Provides a simple endpoint to generate JWT tokens for frontend development without
 * requiring the full GitHub OAuth2 flow.
 */
@RestController
@RequestMapping("/api/v1/dev")
public class DevAuthController {

  private final JwtService jwtService;
  private final UserRepository userRepository;

  public DevAuthController(JwtService jwtService, UserRepository userRepository) {
    this.jwtService = jwtService;
    this.userRepository = userRepository;
  }

  @PostMapping("/login")
  public ResponseEntity<Map<String, String>> login(
      @RequestParam(defaultValue = "dev_admin") String username,
      @RequestParam(defaultValue = "true") boolean isAdmin) {
      
    // Find or create the dev user
    User user = userRepository.findByGithubId(username).orElseGet(() -> {
        User newUser = new User();
        newUser.setGithubId(username);
        newUser.setEmail(username + "@dev.local");
        newUser.setUsername("Developer User");
        newUser.setRole(isAdmin ? Role.ADMIN : Role.USER);
        return userRepository.save(newUser);
    });

    String token = jwtService.generateToken(user);

    return ResponseEntity.ok(Map.of("token", token));
  }
}

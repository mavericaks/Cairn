package com.cairn.security;

import java.util.UUID;

/** WHY: Exposes user information to the client safely without exposing internal entity state. */
public record UserDto(UUID id, String githubId, String email, String username, Role role) {
  public static UserDto fromEntity(User user) {
    return new UserDto(
        user.getId(), user.getGithubId(), user.getEmail(), user.getUsername(), user.getRole());
  }
}

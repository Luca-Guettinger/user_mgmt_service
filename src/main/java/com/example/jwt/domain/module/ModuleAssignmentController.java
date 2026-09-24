package com.example.jwt.domain.module;

import com.example.jwt.domain.user.UserService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Assigns a module to a user (Aufgabe 6).
 *
 * <p>PUT and not POST, and 204 and not 201: the module service stores the assignment idempotently,
 * so repeating the request is not an error and creates nothing new. Mirroring its contract keeps
 * the two services easy to reason about.
 *
 * <p>Answers 204 when assigned, 404 for an unknown user or an unavailable module, and 503 when the
 * module service cannot be reached.
 */
@Validated
@RestController
@RequestMapping("/users")
public class ModuleAssignmentController {

  private final UserService userService;
  private final ModuleClient moduleClient;

  public ModuleAssignmentController(UserService userService, ModuleClient moduleClient) {
    this.userService = userService;
    this.moduleClient = moduleClient;
  }

  @PutMapping("/{userId}/modules/{moduleId}")
  public ResponseEntity<Void> assign(@PathVariable UUID userId, @PathVariable UUID moduleId) {
    // existsById rather than findById: the latter throws NoSuchElementException, which no
    // handler maps, so an unknown user would come back as 500 instead of 404.
    if (!userService.existsById(userId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          String.format("User '%s' could not be found", userId));
    }

    moduleClient.assign(userId, moduleId);
    return ResponseEntity.noContent().build();
  }

  /** What this user already has, so the portal can show it instead of guessing. */
  @GetMapping("/{userId}/modules")
  public ResponseEntity<List<ModuleDto>> retrieveAssigned(@PathVariable UUID userId) {
    if (!userService.existsById(userId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          String.format("User '%s' could not be found", userId));
    }

    return ResponseEntity.ok(moduleClient.listForUser(userId));
  }
}

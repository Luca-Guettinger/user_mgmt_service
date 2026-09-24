package com.example.jwt.domain.module;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reading the module catalogue. Separate from {@link ModuleAssignmentController}, whose class-level
 * mapping is /users and therefore cannot serve /modules.
 *
 * <p>Answers 200 with the catalogue, or 503 when the module service cannot be reached. Requires a
 * JWT, like everything outside the permit-all list in WebSecurityConfig.
 */
@RestController
@RequestMapping("/modules")
public class ModuleController {

  private final ModuleClient moduleClient;

  public ModuleController(ModuleClient moduleClient) {
    this.moduleClient = moduleClient;
  }

  @GetMapping({"", "/"})
  public ResponseEntity<List<ModuleDto>> retrieveAll() {
    return ResponseEntity.ok(moduleClient.list());
  }
}

package com.example.jwt.domain.module;

import java.util.UUID;

/**
 * One module, as the module service reports it. Its JSON also carries created_at and updated_at,
 * which are simply not mapped here - nothing in the portal needs them.
 */
public record ModuleDto(UUID id, String code, String name, String description) {

}

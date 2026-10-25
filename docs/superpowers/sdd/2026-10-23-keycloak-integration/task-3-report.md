# Task 3 implementation report: Keycloak Admin REST and stable identity links

## Status

Implemented on backend branch `feature/keycloak-integration`. Existing service-account access-token caching remains keyed by the active Keycloak configuration and expiry. Admin REST calls continue to use the configured realm and service-account token; token and secret values are not logged.

## Changes

- Extended `KeycloakAdminClient` with idempotent user upsert and enable/disable operations, hierarchical group creation/update, paginated group membership reads, membership updates, effective realm/client role reads, and replacement of direct application-client role mappings.
- Added `KeycloakDirectorySyncService` to reconcile Keycloak users, groups, memberships, and role mappings into workflow-compatible OA projections.
- Reused `keycloak_object_links` to retain the unique mapping from Keycloak subject/group/role IDs to local numeric IDs. Existing mapped IDs are updated in place. Name-based matching is scoped to the active tenant when linking an existing user or department.
- Disabled users remain in OA with their original numeric ID for audit/history references. Synchronizing or disabling users clears local password/salt values so OA credentials do not remain an authentication source.
- Group membership and effective role projections are reconciled from Keycloak. Built-in or otherwise unlinked Keycloak roles are omitted from local OA role projections; Keycloak remains the authority for access decisions.
- Kept `JpaAuditorAware` and `KeycloakOidcUserService` behavior intact: OIDC `sub` already resolves through the stable `KeycloakObjectLink` to the local OA user ID, which the principal exposes to auditing.
- Added repository lookups needed to match department hierarchy, user tenant, and current group membership without changing entity schemas or tenant filters.

## Files

- `KeycloakAdminClient.java`
- `KeycloakDirectorySyncService.java`
- `KeycloakGroupProfile.java`
- `KeycloakUserProfile.java`
- `DepartmentRepository.java`
- `DepartmentUserRepository.java`
- `UserRepository.java`

## Verification

- `mvn -q -DskipTests compile` completed with exit code 0. The only output was the existing JDK/Guice `sun.misc.Unsafe` deprecation warning.
- `git diff --check` completed with no whitespace errors.
- No tests were added or run, per instruction.

## Self-review and limits

- No tenant entity, tenant field, tenant filter, tenant initialization, or existing OA numeric ID was changed.
- Legacy local field widths are retained. Usernames longer than the existing 20-character column are rejected; display name, email, and mobile values are shortened to their existing local projection widths while Keycloak remains authoritative.
- The current group sync supports nested group creation and updates within the same parent. It rejects a request to change an existing group's parent rather than silently diverging the local tree from Keycloak. Task 4 must handle the organization UI/API behavior for group reparenting.
- Keycloak Admin API behavior was not exercised against a live Keycloak server; this task was verified by compilation and static review only.

## Review fix round

- Require an existing tenant ID equal to the active tenant before reusing a linked local user, department, or role projection. Null-tenant projections no longer pass as unscoped matches.
- Resolve existing Keycloak users and groups before writes. Validate linked local ownership and any parent group's ownership before calling the upsert endpoints. The Admin client rechecks the expected external ID immediately before mutation so a newly matching identity cannot be updated after a concurrent change.
- Validate user ownership before enable/disable and role-mapping writes. Validate both the target user and all currently mapped local roles before replacement. Group membership writes validate the group, target user, and current members before changing Keycloak.
- Re-resolve the username or group parent/name path immediately before upsert and compare that natural-key result with the preflight result; explicit object IDs no longer make the expected-ID check tautological. Search Keycloak group pages until exhausted so matching groups beyond the first page remain discoverable.
- Commits: `b0069d1` (`fix: enforce tenant scope in directory sync`), `a1cb98e` (`fix: validate tenant scope before Keycloak writes`), and `427d53b` (`fix: recheck directory identity matches`).
- Verification after the fixes: `mvn -q -DskipTests compile` passed with only existing Guice `Unsafe` deprecation warnings; `git diff --check` passed with Git line-ending notices. No tests were added or run.
- Review: all reported tenant-ordering, natural-key recheck, and pagination findings were addressed; scoped re-reviews found no new issues.

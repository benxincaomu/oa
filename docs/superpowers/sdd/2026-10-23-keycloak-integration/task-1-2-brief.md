# Tasks 1¨C2: Keycloak configuration and OIDC session authentication

## Task Description

Complete plan Tasks 1 and 2 as one integration unit, including the partial implementation already in the branch.

- First-run setup accepts Keycloak issuer/realm, OAuth2 app client, service-account client, and callback settings. Validate OIDC discovery and Admin API access before saving; encrypt secrets with AES-GCM using a required deployment master key; expose only secret-free status; preserve tenant bootstrap semantics.
- `/oauth2/authorization/keycloak` performs OAuth2/OIDC authorization-code login and creates a Spring session. Resolve the client registration from persisted settings and refresh discovery when settings change. Use secure session cookies, logout the Keycloak SSO session, keep initialization and health endpoints public, map Keycloak realm/client roles to Spring authorities, and stop using the Redis token filter for active authentication.
- Browser API calls use same-origin HttpOnly session cookies and CSRF protection; browser code must not read or log access tokens or client secrets. Replace local password login with a Keycloak redirect.

## Global Constraints (verbatim)

- Do not change tenant schema, tenant filtering, or tenant initialization semantics.
- Keep existing numeric OA user and department IDs stable for workflow/audit references.
- Keycloak is authoritative for credentials, users, groups, role assignments, and authorization policies.
- Never expose client secrets, service-account credentials, or access tokens to browser logs or public configuration responses.
- OA remains authoritative for workflow definitions, form schemas, instances, and history.

## Context

Workspace `D:\workspace\oa-p`; backend `D:\workspace\oa-p\oa`; frontend `D:\workspace\oa-p\oa-web`. Both are on the user-requested `feature/keycloak-integration` branch. Inspect and preserve the current partial changes. Design: `D:\workspace\oa-p\docs\superpowers\specs\2026-10-23-keycloak-integration-design.md`.

Known issue to inspect: KeycloakAdminClient caches a management access token without checking CachedToken.configurationKey before returning it after configuration changes. Check authorizedGet/Post/Put request types and configuration/bootstrap order too.

## Instructions

- Keep changes scoped to Tasks 1¨C2; don't start management API migration from Tasks 3¨C6.
- Don't change tenant schema, filters, tenant IDs, or initialization semantics.
- Don't add or run tests. Non-test compilation/build checks are allowed.
- Commit completed changes in each affected repository using focused staging. Do not push.
- Self-review and write the detailed report to the path supplied by the controller.

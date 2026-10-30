# Keycloak cutover and migration

This document describes the OA state after the Keycloak authoritative-directory
cutover, how to initialize a fresh deployment, how to move an existing OA
installation over, and how to roll back.

## Ownership after the cutover

| Concern | Source of truth | OA local storage |
|---|---|---|
| Authentication, credentials, SSO session | Keycloak (OIDC authorization code) | Spring Security session only |
| Keycloak connection settings (issuer, clients, secrets) | application configuration / environment variables (`app.keycloak.*`) | none (never persisted in the database) |
| Users, user attributes | Keycloak Users | numeric-ID projection + display snapshot (`users`) |
| Departments / organization tree | Keycloak Groups (`code`, `leader_user_id` attributes) | numeric-ID projection + snapshot (`department`, `department_user`) |
| Roles | Keycloak application client roles | numeric-ID projection (`role`) |
| Menus, API permissions, role grants | Keycloak Authorization Services resources/policies/permissions | none (no `permission` / `role_permission` writes) |
| Tenant record | OA (`tenant`) | unchanged |
| Workflow definitions, instances, dynamic form tables, history | OA | unchanged; numeric user/department IDs keep their meaning |
| Keycloak object to numeric-ID mapping | OA | `keycloak_object_links` |

The local `users` / `department` / `role` rows are **only** numeric-ID mappings
and display snapshots maintained by `OaNumericIdService`. All management writes
go to Keycloak. The old legacy routes (`/user`, `/organize`, `/role`,
`/permission` writes) fail with a "managed by Keycloak" error.

## Runtime behavior

- Authentication: `/oauth2/authorization/keycloak`; the tenant is resolved from
  the user's Keycloak group that carries the `tenant_id` attribute. A user who is
  not a member of any tenant group is rejected at login.
- Authorization: every non-exempt request is matched against an `oa-api`
  authorization resource and checked through UMA with the user's access token.
  **Fail closed**: no registered resource or a Keycloak error denies the request.
- Menus: built from the `oa-menu` resources granted to the user's client roles.
- Exempt from UMA (authenticated only): `/login/myInfo`, `/login/getMyMenus`,
  `/login/logout`, `/file/**`, `/user/getAllUsers`,
  `/user/findUsersByNameLike/**`, `/organize/listTree`, `/organize/listAll`,
  `/organize/getUsersByDeptId/**` (read-only directory pickers used by workflow
  forms).

## Configuration

Keycloak connection settings are **not** stored in the database. They are read from
`app.keycloak.*` in `application.yml` (or a profile-specific file) and can be
overridden by the matching environment variables:

```yaml
app:
  keycloak:
    issuer-uri: ${KEYCLOAK_ISSUER_URI:}
    realm: ${KEYCLOAK_REALM:}
    client-id: ${KEYCLOAK_CLIENT_ID:}
    client-secret: ${KEYCLOAK_CLIENT_SECRET:}
    service-account-client-id: ${KEYCLOAK_SERVICE_ACCOUNT_CLIENT_ID:}
    service-account-client-secret: ${KEYCLOAK_SERVICE_ACCOUNT_CLIENT_SECRET:}
    redirect-uri: ${KEYCLOAK_REDIRECT_URI:}
    post-logout-redirect-uri: ${KEYCLOAK_POST_LOGOUT_REDIRECT_URI:}
```

- `issuer-uri` must point at the realm (`https://<host>/realms/<realm>`). The checked-in
  `application.yml` already contains all `app.keycloak.*` entries; the OAuth2 redirect
  URIs default to the local frontend (`http://10.0.0.130:4000`). Fill the remaining
  values (issuer, realm, client IDs, secrets) or override them with the environment
  variables shown above.
- `client-id` / `client-secret` are the OAuth2 confidential client used by the
  browser login; `redirect-uri` is usually
  `https://<oa-web-host>/server/login/oauth2/code/keycloak`.
- `service-account-client-id` / `service-account-client-secret` are used for Admin
  REST calls and authorization-resource management.
- Secrets are read into memory only and are never returned by status endpoints or
  written to logs. Prefer environment variables for production instead of
  committing secrets to `application.yml`.
- `SESSION_COOKIE_SECURE=true` when served over HTTPS.

## Prerequisites

1. Keycloak realm reachable over HTTP(S).
2. OAuth2 confidential client for the OA web login (authorization code).
3. Service-account client with `realm-admin`, or with `manage-users`,
   `manage-clients`, and `manage-authorization` realm-management roles.
4. Authorization Services must be enabled on the OAuth2 client. Fresh
   initialization enables it automatically; for an existing deployment call
   `PUT /identity/authorization-baseline` once as an administrator, or enable
   "Authorization" in the Keycloak client settings.
5. The `app.keycloak.*` settings above are configured (and `SESSION_COOKIE_SECURE`
   when behind HTTPS).

## Fresh installation

1. Fill `app.keycloak.*` in `application.yml`, or export the matching environment
   variables, then start the backend. Start the frontend with `API_BASE_URL`
   pointing at the backend.
2. The backend checks the configuration and the database at startup. When Keycloak
   is not fully configured it logs a `KEYCLOAK NOT CONFIGURED` block; when it is
   configured but no tenant exists it logs `OA INITIALIZATION REQUIRED` with the
   initialization URL computed from `app.base-url` (`{app.base-url}/init`).
3. Open that URL. The database has no tenant yet, so the page runs in fresh mode and
   only asks for the root department and the administrator user; the Keycloak
   connection values come from the configuration file. Submit: the backend
   validates OIDC discovery and Admin API access, creates the administrator in
   Keycloak (client role `oa-admin`), creates the tenant root group with the
   `tenant_id` attribute, provisions the baseline menu/API resources, and grants
   them to `oa-admin`.
4. Log in through Keycloak. Additional users are created in the admin UI (or
   directly in Keycloak) and must be members of a tenant group.

## Migrating an existing OA installation

> Back up the database first. The backfill below is idempotent and can be
> re-run; rollback is described at the end.

The goal is to keep every workflow/audit numeric reference valid, so the numeric
IDs already stored in workflow tables must be reused. `keycloak_object_links` is
the bridge; a user or group that is created in Keycloak without a link gets a new
numeric ID on first use.

0. **Configure Keycloak in the application config.** Fill `app.keycloak.*` in
   `application.yml` / environment variables and restart the backend. Until all
   required fields are set the startup log prints `KEYCLOAK NOT CONFIGURED`. When
   the configuration is complete but the database already contains tenants whose
   administrator / root department / admin role are not yet linked to Keycloak,
   startup prints `OA INITIALIZATION REQUIRED` with the initialization URL
   (`{app.base-url}/init`), and business APIs stay unavailable. Open that URL: the
   wizard detects the existing tenant and runs in "existing installation" mode.
   It does not ask for Keycloak settings (they come from the configuration file);
   it validates them, creates/finds the administrator user, the tenant root group,
   and the `oa-admin` application role, links those to the existing numeric
   administrator / root-department / admin-role IDs, sets `tenant_id` and
   `leader_user_id` on the root group, and provisions the baseline menu/API
   resources (an already-existing Keycloak user is not given a new password).
1. **Create groups for the department tree** mirroring the remaining `department`
   rows, with `code` attributes (see the `KeycloakObjectLink` mapping below). The
   tenant root group already exists from step 0.
2. **Create users** for existing `users` rows: username, first/last name, email,
   enabled state. Password hashes are not portable; create users with a temporary
   password or send Keycloak's "update password" action. Place each user in the
   group(s) that match `department_user`.
3. **Create application client roles** for existing OA roles and assign them to
   the corresponding users.
4. **Backfill links** so old numeric IDs are reused (see SQL below). The
   administrator, tenant root department, and `oa-admin` role are already linked
   by step 0; link the remaining users/groups/roles.
5. **Set group leaders** for "starter's department leader" approval:
   `PUT /identity/groups/{groupId}/leader` with `{"userId": "<keycloak user id>"}`.
6. **Republish workbenches** (publish each existing workbench once) so its menu and
   API resources are registered in Keycloak and granted to `oa-admin`. Grant other
   roles through the role page.
7. Verify: login, menus, start a flow, todo/done lists, approval, history names.

### Object link backfill

```sql
-- Example: link an imported Keycloak user to the existing numeric OA user
INSERT INTO keycloak_object_links (object_type, external_id, local_id)
VALUES ('USER', '<keycloak-user-uuid>', <existing-oa-user-id>);
-- Same for 'DEPARTMENT' (Keycloak group UUID -> department.id)
-- and 'ROLE' (Keycloak client role UUID -> role.id).
-- The unique constraints reject duplicate external or local links.
```

`external_id` for a user is the Keycloak subject (user UUID), for a department a
group UUID, and for a role a client-role UUID.

### Tenant marker on the root group

```sql
-- Not applicable if set through the Admin API / Keycloak console;
-- shown as the equivalent JSON on the group representation:
-- attributes: { "tenant_id": ["<numeric tenant id>"], "code": ["<department code>"] }
```

## Rollback

1. Stop both applications.
2. Restore the database backup (workflow data is never modified by the cutover,
   but the backup also restores the projection rows and links).
3. Delete the Keycloak objects created for OA if the realm is shared:
   resources with names starting `oa:`, `oaapi:`, `oa-policy-role-`, and
   `oa-perm:`; optionally the imported users/groups/roles.
4. Deploy the previous backend/frontend build and clear the `app.keycloak.*`
   configuration (the previous build does not use it).
5. The old local role/permission tables were not modified beyond removing new
   writes, so the pre-cutover build can read them again.

## Notes and follow-ups

- The legacy `role` and `permission` controllers were removed; `permission`,
  `role_permission`, and `user_role` rows are no longer read or written by the
  application and can be dropped after the migration is verified. The `users`,
  `department`, and `role` tables remain as numeric-ID projections/snapshots with
  no management endpoints.
- Legacy classes `LoginController` (`legacy-auth` profile), `SecurityFilter`,
  `OaAuthManager`, `SaltedUser`, and `TokenValue*` are inactive but still present;
  they can be removed in a later cleanup.
- Keycloak client secrets live in `application.yml`/environment variables. Do not
  commit production secrets; rotate them in the Keycloak console and update the
  configuration (a restart is required).
- Keycloak availability is required for management writes and for authorization
  decisions; the failure mode is an explicit error / denied request, never a
  silent fallback to local grants.

## First live-Keycloak verification checklist

Run this once against a real realm before production. Items not covered by the
offline compile/build checks are marked as unverified assumptions.

1. Startup and prerequisites
   - All `app.keycloak.*` fields are set (via `application.yml` or the matching
     environment variables); with a field missing, startup logs
     `KEYCLOAK NOT CONFIGURED` and business APIs stay unavailable.
   - The database does not need a Keycloak configuration table; the former
     `keycloak_integration_config` table is no longer read or written.
   - If startup fails on `OAuth2AuthorizedClientManager`, the Security 6.5
     auto-configured bean is missing in this deployment; define one explicitly
     (authorization code + refresh token providers).
2. Initialization
   - `GET /initProject/status` reports `keycloakConfigured`, `initialized`, and
     `keycloakLinked`; `/init` uses it to choose fresh / existing / done mode.
   - `/init` succeeds; Keycloak contains the administrator user, the tenant root
     group with `tenant_id` and `leader_user_id` attributes, client role
     `oa-admin`, the baseline resources (`oa:<tenant>:menu:*`, `oaapi:<tenant>:*`),
     the role policy `oa-policy-role-oa-admin`, and the `oa-perm:*` permissions.
   - Local DB contains the tenant, the administrator snapshot rows, and three
     `keycloak_object_links`; `permission` / `role_permission` / `user_role` stay
     empty.
   - Re-submitting `/init` returns "already initialized" without duplicating
     Keycloak objects; a tenant-only database with missing links enters existing
     mode and only fills links/attributes/baseline resources.
3. Authentication and sessions
   - Anonymous `GET /server/file/baseUrl` and `/server/user/getAllUsers` return
     401/302, never 200.
   - OIDC login works; a Keycloak user created outside a tenant group is rejected
     with a clear error; adding the user to a tenant group allows login and
     allocates a numeric OA ID.
   - After the session expires, a page action redirects to the Keycloak login page
     instead of showing a generic network error.
4. Authorization and menus
   - `GET /server/login/getMyMenus` returns the baseline menu for `oa-admin`;
     a non-admin role sees only what was granted on the role page.
   - Directly calling a registered URL without the role grant returns 403.
   - A URL with no registered `oa-api` resource returns 403 (fail closed).
   - UMA assumptions: `POST {issuer}/protocol/openid-connect/token` with
     `grant_type=uma-ticket`, `audience=<clientId>`, `permission=<resource name>`,
     `response_mode=decision` returns `{"result":true}` or 403. Resource
     permissions were created with resource/policy **IDs**; verify a granted
     decision and a denied one.
   - Stopping Keycloak makes protected requests fail closed (403), not fall back.
5. Directory management
   - The organization page lists the Keycloak group tree; a group created in the
     Keycloak console appears after refresh (proves live reads).
   - Create department / user / role, assign membership and roles, set a group
     leader, reset a password; all changes are visible in the Keycloak console.
   - With Keycloak stopped, management writes fail with a clear error.
   - Group `leader_user_id` attributes are updated through the group attribute
     write; confirm Keycloak accepts the whole-group PUT.
6. Workflow
   - Publish a workbench: menu entries appear for `oa-admin`; resources
     `oa:<tenant>:mengroup:wb:*`, three menu pages, and the two `/flowForm|workflow
     /{id}/**` API resources exist in Keycloak.
   - Start a flow with an explicit assignee, a department candidate group, and the
     "starter's department leader" assignment; each task reaches the right user
     (numeric IDs remain in BPMN / form-assignee rows).
   - A user in two Keycloak groups sees candidate tasks of both groups under "my
     todo".
   - Approve, check "my done"; history rows show the Keycloak display name
     (truncated to the column limit) and survive a later Keycloak rename.
   - Re-publish and delete a workbench: resources are updated/removed and workflow
     data/history remain.
   - Force a publication failure (e.g. revoke `manage-authorization`): the
     publication fails, its Camunda deployment is removed, and a retry succeeds.
7. Rollback
   - Follow the rollback section above and confirm the previous build can read the
     local tables again.

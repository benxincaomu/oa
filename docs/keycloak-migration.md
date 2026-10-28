# Keycloak cutover and migration

This document describes the OA state after the Keycloak authoritative-directory
cutover, how to initialize a fresh deployment, how to move an existing OA
installation over, and how to roll back.

## Ownership after the cutover

| Concern | Source of truth | OA local storage |
|---|---|---|
| Authentication, credentials, SSO session | Keycloak (OIDC authorization code) | Spring Security session only |
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
  `/login/logout`, `/file/**`, `/user/currUser`, `/user/getAllUsers`,
  `/user/findUsersByNameLike/**`, `/organize/listTree`, `/organize/listAll`,
  `/organize/getUsersByDeptId/**` (read-only directory pickers used by workflow
  forms).

## Prerequisites

1. Keycloak realm reachable over HTTP(S).
2. OAuth2 confidential client for the OA web login (authorization code).
3. Service-account client with `realm-admin`, or with `manage-users`,
   `manage-clients`, and `manage-authorization` realm-management roles.
4. Authorization Services must be enabled on the OAuth2 client. Fresh
   initialization enables it automatically; for an existing deployment call
   `PUT /identity/authorization-baseline` once as an administrator, or enable
   "Authorization" in the Keycloak client settings.
5. Environment variables:
   - `KEYCLOAK_SETUP_TOKEN` — one-time setup token required by the init endpoint.
   - `KEYCLOAK_CONFIG_ENCRYPTION_KEY` — master key used to encrypt client secrets
     at rest (required; setup fails without it).
   - `SESSION_COOKIE_SECURE` — `true` behind HTTPS.
   - `KEYCLOAK_SETUP_TOKEN` must never be exposed in browser logs or the UI.

## Fresh installation

1. Start the backend with the environment variables above and the frontend with
   `API_BASE_URL` pointing at the backend.
2. The backend checks the database at startup. When
   `keycloak_integration_config` is empty (or the tenant is missing), the log prints
   an `OA INITIALIZATION REQUIRED` block with the initialization URL computed from
   `app.base-url` (`{app.base-url}/init`). Open that URL and fill the Keycloak
   settings (issuer, realm, OAuth2 client, service-account client, callback URLs,
   setup token), the root department, and the administrator user.
3. Submit. The backend validates OIDC discovery and Admin API access, creates the
   administrator in Keycloak (client role `oa-admin`), creates the tenant root
   group with the `tenant_id` attribute, provisions the baseline menu/API
   resources, and grants them to `oa-admin`.
4. Log in through Keycloak. Additional users are created in the admin UI (or
   directly in Keycloak) and must be members of a tenant group.

## Migrating an existing OA installation

> Back up the database first. The backfill below is idempotent and can be
> re-run; rollback is described at the end.

The goal is to keep every workflow/audit numeric reference valid, so the numeric
IDs already stored in workflow tables must be reused. `keycloak_object_links` is
the bridge; a user or group that is created in Keycloak without a link gets a new
numeric ID on first use.

1. **Create the tenant root group** in Keycloak (name = existing root department),
   then set its `tenant_id` attribute to the numeric OA tenant ID (one-time SQL
   template below, or use the Admin API).
2. **Create groups for the department tree** mirroring `department` rows, with
   `code` attributes (see the `KeycloakObjectLink` mapping below).
3. **Create users** for existing `users` rows: username, first/last name, email,
   enabled state. Password hashes are not portable; create users with a temporary
   password or send Keycloak's "update password" action. Place each user in the
   group(s) that match `department_user`.
4. **Create application client roles** for existing OA roles and assign them to
   the corresponding users.
5. **Backfill links** so old numeric IDs are reused (see SQL below).
6. **Set group leaders** for "starter's department leader" approval:
   `PUT /identity/groups/{groupId}/leader` with `{"userId": "<keycloak user id>"}`.
7. **Provision baseline resources**: call `PUT /identity/authorization-baseline`
   once as an administrator.
8. **Republish workbenches** (publish each existing workbench once) so its menu and
   API resources are registered in Keycloak and granted to `oa-admin`. Grant other
   roles through the role page.
9. Verify: login, menus, start a flow, todo/done lists, approval, history names.

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
4. Deploy the previous backend/frontend build and keep the legacy
   `KEYCLOAK_*` configuration disabled.
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
- Do not expose `KEYCLOAK_SETUP_TOKEN` after initialization.
- Keycloak availability is required for management writes and for authorization
  decisions; the failure mode is an explicit error / denied request, never a
  silent fallback to local grants.

## First live-Keycloak verification checklist

Run this once against a real realm before production. Items not covered by the
offline compile/build checks are marked as unverified assumptions.

1. Startup and prerequisites
   - `KEYCLOAK_SETUP_TOKEN`, `KEYCLOAK_CONFIG_ENCRYPTION_KEY`, and
     `SESSION_COOKIE_SECURE` are set; the application starts.
   - If startup fails on `OAuth2AuthorizedClientManager`, the Security 6.5
     auto-configured bean is missing in this deployment; define one explicitly
     (authorization code + refresh token providers).
2. Initialization
   - `/init` succeeds; Keycloak contains the administrator user, the tenant root
     group with a `tenant_id` attribute, client role `oa-admin`, the baseline
     resources (`oa:<tenant>:menu:*`, `oaapi:<tenant>:*`), the role policy
     `oa-policy-role-oa-admin`, and the `oa-perm:*` permissions.
   - Local DB contains the tenant, the administrator snapshot rows, and three
     `keycloak_object_links`; `permission` / `role_permission` / `user_role` stay
     empty.
   - Re-submitting `/init` returns "already initialized" without duplicating
     Keycloak objects.
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

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
2. Open `/init`, fill the Keycloak settings (issuer, realm, OAuth2 client,
   service-account client, callback URLs, setup token), the root department, and
   the administrator user.
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

- `permission`, `role_permission`, `user_role`, and `permission`-related legacy
  read endpoints are kept read-only for compatibility; they can be dropped after
  the migration is verified.
- Legacy classes `LoginController` (`legacy-auth` profile), `SecurityFilter`,
  `OaAuthManager`, `SaltedUser`, and `TokenValue*` are inactive but still present;
  they can be removed in a later cleanup.
- Do not expose `KEYCLOAK_SETUP_TOKEN` after initialization.
- Keycloak availability is required for management writes and for authorization
  decisions; the failure mode is an explicit error / denied request, never a
  silent fallback to local grants.

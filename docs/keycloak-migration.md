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
| Tenant record | OA (`tenant`) | a single default tenant row is created automatically; Keycloak groups are the organization tree and do **not** need a `tenant_id` attribute |
| Workflow definitions, instances, dynamic form tables, history | OA | unchanged; numeric user/department IDs keep their meaning |
| Keycloak object to numeric-ID mapping | OA | `keycloak_object_links` |

The local `users` / `department` / `role` rows are **only** numeric-ID mappings
and display snapshots maintained by `OaNumericIdService`. All management writes
go to Keycloak. The old legacy routes (`/user`, `/organize`, `/role`,
`/permission` writes) fail with a "managed by Keycloak" error.

## Runtime behavior

- Authentication: `/oauth2/authorization/keycloak`. OA is single-tenant: every
  Keycloak user belongs to the default local tenant, no `tenant_id` group
  attribute is required, and the first login allocates the numeric OA user ID.
  Keycloak groups are the organization tree only.
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

Keycloak connection settings are **not** stored in the database. Like `db.*`,
`redis.*` and `minio.*`, they live in the Maven profile filter files
(`src/main/filters/{oa,oa1,prod}.properties`) and are injected into
`application.yml` through `@keycloak.*@` placeholders:

```properties
# src/main/filters/oa.properties
keycloak.issuer-uri=http://10.0.0.102:8080/realms/oa
keycloak.realm=oa
keycloak.client-id=oa-web
keycloak.client-secret=<oa-web client secret>
keycloak.service-account-client-id=oa-service
keycloak.service-account-client-secret=<oa-service client secret>
keycloak.redirect-uri=http://10.0.0.130:4000/server/login/oauth2/code/keycloak
keycloak.post-logout-redirect-uri=http://10.0.0.130:4000/app
```

Select the environment with the Maven profile (`-Poa` is the default, `-Poa1` for
QA, `-Pprod` for production); the matching file is filtered into
`target/classes/application.yml` at build time. Environment variables such as
`APP_KEYCLOAK_ISSUER_URI` can still override the filtered values at runtime.

- `issuer-uri` must point at the realm (`https://<host>/realms/<realm>`).
- `client-id` / `client-secret` are the OAuth2 confidential client used by the
  browser login; `redirect-uri` is usually
  `https://<oa-web-host>/server/login/oauth2/code/keycloak`.
- `service-account-client-id` / `service-account-client-secret` are used for Admin
  REST calls and authorization-resource management.
- Secrets are read into memory only and are never returned by status endpoints or
  written to logs. The filter files are committed, so do not put production
  secrets there; override them with environment variables in production.
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

## Startup provisioning (fresh deployment)

There is no initialization page and no `/initProject` endpoint. Every startup
checks and — when missing — creates the Keycloak authorization model through the
Admin REST API.

1. Fill `keycloak.*` in `src/main/filters/<profile>.properties` (or override with
   the matching environment variables), then start the backend and the frontend.
2. On startup OA:
   - creates the single default local tenant row if the database has none;
   - ensures the `oa-admin` application client role exists on the configured
     application client (`oa-web`) and enables Authorization Services there;
   - ensures the baseline menu/API resources, the role policy
     `oa-policy-role-oa-admin` and the `oa-perm:*` resource permissions exist.
   The log prints `Keycloak 角色与权限初始化完成` and business APIs become
   available.
3. If the configuration is incomplete or a Keycloak Admin API call fails, the log
   shows `KEYCLOAK NOT CONFIGURED` or the initialization error and business APIs
   stay unavailable (HTTP 503). Fix the configuration/Keycloak and restart; the
   provisioning is idempotent.
4. Users and groups are managed in Keycloak (or through the OA user/organization
   pages, which write to Keycloak). Startup does **not** create users, groups or
   administrator identities. Assign `oa-admin` to the Keycloak users who should
   administer OA: Users → the user → Role mapping → Assign role → filter by client
   `oa-web` → `oa-admin`.
5. Log in through Keycloak. A user without a pre-existing OA row or group
   membership can log in; a numeric OA ID is allocated on first use.

## Migrating an existing OA installation

> Back up the database first. The backfill below is idempotent and can be
> re-run; rollback is described at the end.

The goal is to keep every workflow/audit numeric reference valid, so the numeric
IDs already stored in workflow tables must be reused. `keycloak_object_links` is
the bridge; a user or group that is created in Keycloak without a link gets a new
numeric ID on first use.

1. **Configure Keycloak and start the application.** Fill `keycloak.*` in
   `src/main/filters/<profile>.properties` (or environment variables) and start the
   backend. Startup creates/updates the `oa-admin` role, the baseline menu/API
   resources, the role policy and the resource permissions; no page, user, group or
   tenant group is created. Until this succeeds, business APIs stay unavailable
   (see the startup log).
2. **Create groups for the department tree** in Keycloak, mirroring the remaining
   `department` rows (`code` attributes are optional; they are kept for display).
   Top-level groups are fine — no `tenant_id` attribute and no single root group are
   required.
3. **Create users** for existing `users` rows: username, first/last name, email,
   enabled state. Password hashes are not portable; create users with a temporary
   password or send Keycloak's "update password" action. Place each user in the
   group(s) that match `department_user`.
4. **Create application client roles** for existing OA roles and assign them to the
   corresponding users. Assign `oa-admin` to the OA administrators.
5. **Backfill links** so old numeric IDs are reused (see SQL below). Startup does not
   create directory links, so link existing users, groups and roles here.
6. **Set group leaders** for "starter's department leader" approval:
   `PUT /identity/groups/{groupId}/leader` with `{"userId": "<keycloak user id>"}`.
7. **Republish workbenches** (publish each existing workbench once) so its menu and
   API resources are registered in Keycloak and granted to `oa-admin`. Grant other
   roles through the role page.
8. Verify: login, menus, start a flow, todo/done lists, approval, history names.

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
2. Startup provisioning
   - The log shows `Keycloak 角色与权限初始化完成：tenantId=...`; business APIs become
     available without opening any page.
   - Keycloak contains the `oa-admin` client role on the application client, the
     baseline resources (`oa:<tenant>:menu:*`, `oaapi:<tenant>:*`), the role policy
     `oa-policy-role-oa-admin`, and the `oa-perm:*` permissions.
   - Local DB contains exactly one auto-created `tenant` row; startup creates no
     users, groups, departments or `keycloak_object_links`.
   - Restarting the application does not duplicate roles/resources/policies/
     permissions (idempotent).
   - With Keycloak unreachable (or the Admin API permissions missing), startup logs
     the failure, business APIs return 503, and a later restart after fixing the
     issue succeeds.
3. Authentication and sessions
   - Anonymous `GET /server/file/baseUrl` and `/server/user/getAllUsers` return
     401/302, never 200.
   - OIDC login works for a Keycloak user even without any group membership or
     `tenant_id` attribute; the first login allocates a numeric OA ID.
   - Group membership only affects the organization tree and task candidate groups,
     not whether the user can log in.
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

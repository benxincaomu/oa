# SDD ledger - plan: D:\workspace\oa-p\docs\superpowers\plans\2026-10-25-keycloak-authoritative-directory.md

## Workspace and rulings

Ruling: Work in the existing `feature/keycloak-integration` branches. The backend already has related uncommitted work, and the frontend branch is clean; creating another worktree would not share the in-progress backend edits safely. Cost if wrong: less isolation than a fresh worktree.

Ruling: Do not add or run tests. Use diff review and non-test compile/build checks only, as required by the developer instruction and the existing integration ledger. Cost if wrong: behavioral regressions may be found later than with tests.

Ruling: Treat Keycloak profiles, memberships, roles, and permissions as the only current-data source. Keep `keycloak_object_links` only to translate legacy numeric workflow references; do not read local User, Department, Role, or Permission rows for current directory data. Cost if wrong: additional changes are needed if local profile snapshots were expected to remain a supported fallback.

Ruling: Use sequential inline execution because authorization, initialization, API, UI, and workflow tasks consume shared contracts. Cost if wrong: implementation may take longer than truly independent parallel work.

## Preflight dependency scan

| Tasks | Shared file/interface | Producer -> consumer | Finding |
|---|---|---|---|
| 1 -> 2 | `KeycloakAuthorizationService` | AuthZ resource/policy API -> initialization seeds | Sequential; Task 2 consumes Task 1's typed, idempotent API. |
| 1 -> 4 | stable resource keys and attributes | AuthZ service -> workbench registration | Compatible if the same tenant/workbench/resource key format is used. |
| 1 -> 5 | AuthZ decision API | Admin client/service -> request authorization manager | Sequential; Task 5 must use the same client and scope semantics. |
| 2 -> 3 | tenant root group and Keycloak links | initialization -> tenant-scoped reads | Compatible; Keycloak tenant markers must be established before API filtering. |
| 3 -> 5 | OIDC subject and Keycloak-backed profiles | directory API/principal -> menu and authorization checks | Compatible; authentication uses `sub`, current directory fields come from Keycloak APIs. |
| 3 -> 6 | identity API DTOs and string external IDs | backend API -> frontend management pages | Sequential; stabilize endpoint fields before UI migration. |
| 3 -> 7 | opaque legacy-ID mapping | directory API -> workflow legacy references | Compatible; mapping translates IDs and never supplies profile data. |
| 4 -> 5 | resource metadata/scopes | workbench publish -> runtime route check | Compatible; published paths must match authorization resource URIs and scopes. |
| 5 -> 6 | authorized menu and endpoint contracts | runtime auth -> frontend navigation | Compatible; UI consumes already-filtered menu results. |
| 1 | `KeycloakAdminClient`, `KeycloakAuthorizationService`, DTOs | AuthZ CRUD and stable keys | Agrees with itself; API operations stay behind a typed service. |
| 2 | `InitController`, `InitService`, bootstrap identity | KC seeds before initialized state | Agrees with itself; no local role/permission seed is retained. |
| 3 | identity APIs, OIDC principal, legacy controllers | direct Keycloak data access | Agrees with itself; local rows are not directory read sources. |
| 4 | `WorkbenchService` and publish state | dynamic KC resource reconciliation | Agrees with itself; updates and retries use stable resource keys. |
| 5 | `SecurityConfig`, authorization manager, menus | backend authorization and menu retrieval | Agrees with itself; fail closed for protected routes. |
| 6 | admin UI pages and DTOs | Keycloak-backed API contracts | Agrees with itself; no local role/permission IDs remain in UI state. |
| 7 | auditor, workflow services/repositories, ID link | existing workflow data and KC current identity | Agrees with itself; legacy references remain resolvable. |
| 8 | all changed files and migration docs | implementation verification | Agrees with itself; only compile/build/diff checks are run. |

## Existing workspace state

- Backend branch is `feature/keycloak-integration` with pre-existing uncommitted changes in `KeycloakAdminClient`, `KeycloakDirectorySyncService`, `KeycloakObjectLinkRepository`, `SecurityConfig`, user/organization controllers/repositories, and new identity controller/role DTO files. These changes are in scope and must be preserved and reviewed.
- Frontend branch is `feature/keycloak-integration` and was clean at plan start.
- Existing integration ledger marks Tasks 1-3 complete and Task 4 onward incomplete. This plan supersedes its projection-as-current-data behavior according to the user's latest explicit direction.

## Progress

- [x] Task 1 / W1: Keycloak Authorization Services layer
- [x] Task 2 / W2: baseline initialization in Keycloak
- [x] Task 3 / W3: Keycloak-only identity APIs and no projection reads
- [x] Task 4-5 / W5: dynamic workbench resources, runtime authorization and menus
- [x] Task 6 / W6: administration UI migration
- [x] Task 7 / W4: workflow ID compatibility
- [x] Task 8 / W6: review and non-test verification

All work packages are complete. Final verification: backend
`mvn -q -DskipTests compile` exit 0, frontend `next build` exit 0, and
`git diff --check` exit 0 in both repositories (no Keycloak integration run,
per the approved constraint).

## Round log

### Round 1 (2026-10-25): decisions + W1

- Decisions recorded in `decisions.md` (default grant -> `oa-admin`, local tables
  stay as read-only ID mapping/snapshots, no Keycloak integration testing).
- Added `OaAuthorizationKeys`, `OaStoredResource`, and
  `KeycloakAuthorizationService`: idempotent menu group/page and API-pattern
  resources, one role policy per application client role, one resource
  permission per (resource, role), batch grant/revoke, workbench cleanup,
  granted-resource lookup for menus.
- `KeycloakAdminClient` gained a query-parameter authz read, a POST helper,
  cached client UUID resolution, and a cached `ensureAuthorizationServices()`.
- `mvn -q -DskipTests compile` passes with the new code.
- Admin REST assumptions that need later integration verification:
  - `GET /resource`, `GET /policy`, `GET /permission` accept a `name` filter and
    the matches are re-checked exactly on the client side.
  - resource permissions are created with resource **names** in `resources` and
    policy **names** in `policies` (no scopes in this cutover).
  - create calls return the new ID through the `Location` header, with
    `id`/`_id` in the body as fallback.
- Sandbox note: only the workspace root is writable by shell child processes,
  so Maven must run with a one-shot wider sandbox approval
  (`danger-full-access`); file edits through the editor tools are unaffected.

### Round 2 (2026-10-25): W2 baseline initialization in Keycloak

- Added `OaBaselineAuthorizationCatalog` (baseline menus and API patterns copied
  from the old local seed) and `OaBaselineAuthorizationProvisioner` (idempotent
  provisioning + `oa-admin` default grant).
- `InitService` no longer seeds `permission`, `role_permission`, or `user_role`;
  it keeps only the tenant record, the administrator user/role/department rows as
  numeric-ID snapshots, their object links, the group `tenant_id` attribute, and
  the baseline authorization provisioning.
- `KeycloakAdminClient.bootstrapAdministrator` no longer creates a parallel realm
  role; the initial administrator gets only the application client role
  `oa-admin` (exposed as `OA_ADMIN_CLIENT_ROLE`). Added `setGroupAttribute`.
- Removed the now unused realm-role helpers and the unused local permission
  seeding code.
- `mvn -q -DskipTests compile` passes.
- Known intermediate state: `getMyMenus` and the legacy role/permission pages
  still read local tables, so menus are empty until W3/W5 move them to Keycloak.

### Round 3 (2026-10-25): W3 Keycloak-only directory reads

- Added `OaKeycloakGroup` and Admin REST group reads with attributes
  (`listGroupsWithAttributes`, `listUserGroups`) plus numeric attribute parsing.
- Added `OaNumericIdService`: the only writer of local numeric-ID projections and
  display snapshots; allocates/reuses OA IDs for Keycloak users, groups, roles.
- Added `KeycloakDirectoryService`: tenant scope derived from the group carrying
  the `tenant_id` attribute (descendants inherit), Keycloak-only user/group/role
  reads, numeric-ID department tree and user pickers, and management writes that
  never touch local directory rows. New users created through it are placed in
  the tenant root group so they can log in immediately.
- Rewrote `IdentityManagementController` on top of the new directory service;
  same endpoint shapes as before.
- Rewrote `OaOidcUserService`: login resolves the tenant from Keycloak groups and
  creates/reuses the numeric ID on first login. A Keycloak-created user works
  without any pre-existing local row; users outside every tenant group are
  rejected.
- Shut down local identity/authorization writes on legacy routes:
  `/user` insert/update/disable/assignRole/assignDept/setPassword/getRoleId,
  `/organize` insert/assignLeader, `/role` insert/auth now fail with a clear
  "managed by Keycloak" error. Workflow-facing reads
  (`/user/getAllUsers`, `/user/findUsersByNameLike`, `/organize/listTree`,
  `/organize/listAll`, `/organize/getUsersByDeptId`) now return Keycloak data
  mapped to numeric IDs.
- Deleted the obsolete `KeycloakDirectorySyncService` (write-on-read projection
  sync) after confirming it had no callers.
- `mvn -q -DskipTests compile` passes.
- Known intermediate state: admin list pages (`/user/list`, `/role/list*`,
  `/permission/*`, `/organize/list`) and `getMyMenus` still read local snapshot
  tables until W5/W6.

### Round 4 (2026-10-25): W4 workflow identity compatibility

- `FlowHistoryRepository` resolves the operator display name from Keycloak and
  stores it as the historical `operator_name` snapshot; fixed the pre-existing
  bug where `tenant_id` was written from `getCurrentUserId()`.
- `WorkflowService` resolves the approval operator name from Keycloak and matches
  candidate-group tasks using numeric department IDs of the user's Keycloak
  groups (all groups are tried, not just one local department row).
- `FlowFormService` uses the same Keycloak-group -> numeric department mapping for
  form detail candidate tasks and the todo list.
- `UserTaskCreateListener`: `${starterId}` / legacy `${starterLeader}` now resolve
  the starter's group leader from the Keycloak `leader_user_id` group attribute and
  set that leader as the task assignee (previously the starter ID was recorded);
  falls back to the starter when no leader is configured.
- The BPMN designer now writes `${starterId}` so the assignment expression and the
  listener agree (it previously wrote `${starterLeader}`, which the listener never
  matched).
- New `PUT /identity/groups/{groupId}/leader` stores the OA numeric user ID of a
  group leader in the Keycloak group attribute, keeping the "starter's department
  leader" workflow feature configurable from the new API.
- Confirmed no workflow package class imports the local identity repositories any
  more; workflow tables, dynamic form tables, and BPMN attributes still use the
  same numeric OA IDs.
- `mvn -q -DskipTests compile` passes.

### Round 5 (2026-10-25): W5 dynamic resources, UMA and menus

- `WorkbenchService.publish` now registers (on every publish, idempotently) one
  workbench menu group, three menu pages (`myStart`/`myTodo`/`myDone`), and the
  `/flowForm/{id}/**` + `/workflow/{id}/**` API resources in Keycloak, and grants
  them to `oa-admin` by default. The old local `Permission`/`role_permission`
  writes were removed. Added workbench delete (`DELETE /workbench/{id}`) which
  removes its Keycloak resources and keeps published business/history data.
- Added `KeycloakUamDecisionService`: UMA `uma-ticket` decision with the user's
  access token (`response_mode=decision`), short-lived grant cache, fail closed on
  Keycloak errors.
- Added `KeycloakRequestAuthorizationManager`: request paths are matched against
  registered `oa-api` resources and authorized with UMA; unmatched paths and
  Keycloak errors deny. Exemptions are limited to login/self-service, file access,
  and read-only directory pickers used by workflow forms.
- `SecurityConfig` now uses `.anyRequest().access(requestAuthorizationManager)`
  (keeping the coarse `ROLE_oa-admin` guard for `/identity/**`).
- `OaLoginController.getMyMenus` now returns the menu tree built from Keycloak
  authorization resources granted to the user's client roles
  (`KeycloakMenuService`, `OaMenuVo`); the local role/permission read is gone.
- Added Keycloak authorization administration endpoints used by the upcoming UI:
  `GET /identity/resources`, `GET|PUT /identity/roles/{roleName}/resources`
  (administrator grants are protected from replacement).
- Assumptions to verify when a Keycloak environment is available:
  - Spring Security 6.5 auto-registers the `OAuth2AuthorizedClientManager` bean
    (used for access-token refresh) when `oauth2Login` is configured.
  - `POST {issuer}/protocol/openid-connect/token` with
    `grant_type=uma-ticket`, `audience=<clientId>`, `permission=<resource name>`,
    `response_mode=decision` returns `{"result":true}` or 403.
  - resource permissions created with resource/policy names are accepted by the
    Admin REST API.
- `mvn -q -DskipTests compile` passes.

### Round 6 (2026-10-25): W6 frontend migration, docs and final verification

- Rewrote the administration pages against the Keycloak-backed API:
  - user management (`/identity/users`, enable/password/roles/groups),
  - organization management (`/identity/groups`, create + leader),
  - role management (`/identity/roles` + `/identity/resources` +
    `/identity/roles/{role}/resources`; `oa-admin` grants are read-only),
  - permissions page is now a read-only Keycloak resource catalog.
- Menu component accepts Keycloak resource keys as node IDs; the menu itself comes
  from `/login/getMyMenus` (Keycloak-authorized resources).
- The initialization page no longer creates local role data; it now only collects
  Keycloak settings, the root organization, and the initial administrator (role
  field removed from `InitVo`).
- Workbench list wires the new delete endpoint (removes Keycloak resources).
- Removed the obsolete local login page and the local set-password page; no
  frontend code calls the retired local management endpoints any more (verified by
  search).
- Added `docs/keycloak-migration.md`: ownership matrix, prerequisites, fresh
  install, existing-install backfill (including `keycloak_object_links` SQL),
  rollback, and legacy cleanup notes. Added
  `PUT /identity/authorization-baseline` for existing deployments that cannot
  re-run initialization.
- Final cleanup: removed the remaining local directory read on
  `/user/getDeptIdByUserId` and the unreachable local permission insert; normalized
  line endings and trimmed trailing whitespace in the changed sources.
- Verification (no Keycloak environment, as approved):
  - `mvn -q -DskipTests compile` -> exit 0.
  - `next build` (production) -> exit 0.
  - `git diff --check` in `oa` and `oa-web` -> exit 0 (initial pre-existing
    trailing-whitespace lines cleaned).
  - Frontend route list no longer contains `/setPassword`; the retired endpoints
    are absent from `oa-web/src`.
- Residual risk (accepted): Keycloak Admin REST/UMA payload assumptions listed in
  Round 5 can only be confirmed against a live realm; no automated tests were
  added or run.

### Round 7 (2026-10-26): post-review defect fixes

Review of the cutover found and fixed one high-severity security defect and
several functional defects, then started working through the remaining list.

- `fix: harden request authorization and workflow identity display` (00b95b5):
  - authorization exemptions were applied before the authentication check, so
    anonymous requests could reach `/file/**`, `/user/getAllUsers`,
    `/organize/listTree`; authentication is now checked first;
  - `operator_name` (varchar(20)) is truncated before the history insert so long
    Keycloak names cannot break approval;
  - principal and picker display names come from OIDC claims / Keycloak profiles
    instead of the truncated local snapshot;
  - authorization resource types are filtered on the client side.
- `fix: match todo candidate groups across all Keycloak groups of a user`
  (1edcf36): `myToDo` now receives the full department-ID list and builds one
  JSONB candidate-group condition per department.
- `refactor: remove legacy local-read identity endpoints` (8410740): deleted the
  role/permission controllers and reduced the user/department controllers to the
  Keycloak-backed picker reads; removed the class-level `oa-admin` guard on the
  department picker so non-admin initiators can load candidate departments again;
  baseline authorization no longer registers `/role` and `/permission`.
- `perf: cache Keycloak directory reads and adopt existing groups` (26c015b):
  20-second caches for group tree/user index/group members with invalidation on
  every write; O(n) tenant ownership pre-computation; numeric-ID snapshot rows
  only saved when changed; group creation reuses an existing same-path Keycloak
  group instead of failing.

The remaining review items were also fixed:

- `fix: make initialization completion marker transaction-safe` (a48479c):
  explicit initialization lock with a retry error for concurrent calls; the
  initialized marker is only written after the initialization transaction
  commits.
- `fix: compensate failed workbench publications and clean design metadata on
  delete` (79d29e1): a failed publication removes its Camunda deployment; deleting
  a workbench also removes its publish/entity/workflow design rows while keeping
  business data and history.
- `fix: scope authorization resource matching to the caller tenant` (16b30ae):
  `oa-api` resources are matched only for the principal's tenant (resources
  carrying a `tenantId` attribute).
- `fix: handle expired sessions and tidy admin page interactions` (2bb3dff):
  XHR responses that come back as an HTML login page or 401 redirect to the
  Keycloak authorization endpoint; the duplicate enable/disable popconfirm was
  removed and the disabled admin-grant button keeps a working tooltip.
- `docs: add the first live-Keycloak verification checklist` records every
  assumption that needs a real realm (UMA decision payload, resource/policy IDs,
  `OAuth2AuthorizedClientManager`, group attribute PUT) in
  `docs/keycloak-migration.md`.

Verification for this round: backend `mvn -q -DskipTests compile` after each
backend change and one production `next build` after the frontend changes, all
exit 0; every commit passed `git diff --cached --check` (exit 0) before it was
pushed to `origin/feature/keycloak-integration`.

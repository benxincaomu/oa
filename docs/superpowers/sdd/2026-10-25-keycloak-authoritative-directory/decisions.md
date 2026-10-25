# Cutover decisions and refined work plan

Supersedes the open questions in the task list of
`docs/superpowers/plans/2026-10-25-keycloak-authoritative-directory.md`.
Scope: fully move users, organization, roles, permissions, authentication, and
authorization to Keycloak; keep every workflow feature working; let OA request
Keycloak data on demand but stop managing it locally.

## User-approved rulings

1. **Default grant target**: a newly created menu/API resource is granted to the
   application client role `oa-admin` (current behaviour of the tenant's minimum
   role). Other roles must be granted explicitly.
2. **Local directory tables**: keep `users`, `department`, `role`, `permission`,
   `user_role`, `role_permission`, `department_user` as **read-only numeric-ID
   mappings and historical snapshots**; management writes go to Keycloak only.
   Do not drop tables in this cutover.
3. **Verification**: no Keycloak integration environment. Acceptance is
   `mvn -q -DskipTests compile`, frontend production build,
   `git diff --check`, plus code review. Admin REST assumptions that cannot be
   verified locally must be recorded in the progress ledger.

## Numeric-ID compatibility model

- `keycloak_object_links` is the only external-id -> numeric-id mapping.
  It keeps `USER`, `DEPARTMENT`, `ROLE` types; `PERMISSION` is unused and will
  stay unused because permissions are Keycloak resources now.
- Workflow tables, BPMN attributes, and audit columns keep numeric IDs:
  `starterId`, `camunda:assignee`, `camunda:candidateGroups`, `create_by`,
  `flow_form_assignee.assignee/candidate_groups`.
- New Keycloak principals get a numeric ID allocated on first reference and
  stored in the link table; existing local IDs are preserved through a one-time
  backfill (idempotent, documented, reversible).
- Display names required by history are snapshotted into workflow rows
  (`operator_name`); current names are resolved from Keycloak when needed.

## Authorization model

- One Keycloak authorization resource per menu route and per protected API URL
  pattern. Stable names:
  - `oa:<tenantId>:menu:<route>` type `oa-menu`, attribute `value=<route>`,
    `parent=<parent resource name>`, `sortOrder=<n>`, `displayName=<name>`.
  - `oaapi:<tenantId>:<METHOD>:<ant-pattern>` type `oa-api`, attribute
    `pattern=<ant-pattern>`, `method=<METHOD>`, optional `workbench=<key>`.
- No scopes in this cutover: menu and API access are separate resources, which
  keeps Admin REST payloads minimal and version tolerant.
- One reusable client-role policy per OA role: `oa-policy-role-<roleName>` with
  the application client role of the same name.
- One resource permission per (resource, role):
  `oa-perm:<resourceName>#<roleName>`, positive + unanimous, referencing the
  resource and the role policy.
- Default grant: every resource created by initialization or workbench
  publication gets a permission for `oa-admin`.
- Runtime: UMA `uma-ticket` decision using the logged-in user's access token,
  fail closed; menus come from the user's granted `oa-menu` resources.

## Work packages

| # | Package | Plan tasks |
|---|---|---|
| W1 | Keycloak Authorization Services layer (`KeycloakAuthorizationService`) | Task 1 |
| W2 | Baseline provisioning during initialization; drop local seeds | Task 2 |
| W3 | Read-only Keycloak directory adapter, management API, legacy route shutdown | Task 3 |
| W4 | Workflow identity compatibility (numeric IDs, snapshots, leaders, candidate groups) | Task 7 |
| W5 | Workbench dynamic resources, runtime authorization, Keycloak menus | Tasks 4-5 |
| W6 | Frontend migration, migration/rollback docs, verification | Tasks 6, 8 |

## Acceptance mapping to the three user goals

- Goal 1 (workflow preserved): no changes to workflow table structure; existing
  flow instances/history readable; start/todo/done/approve/design/publish work.
- Goal 2 (Keycloak manages directory): all management writes hit Keycloak; OA
  read paths never require or update local directory rows; Keycloak failure
  blocks management writes with a clear error and never falls back to local.
- Goal 3 (interop): Keycloak data reaches OA through the adapter where UUIDs and
  numeric IDs are translated; menus, authorization, task assignment, candidate
  groups, and audit keep working with numeric references.

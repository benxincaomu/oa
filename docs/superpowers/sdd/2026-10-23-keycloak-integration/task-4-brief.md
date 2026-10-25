# Task 4: Keycloak management APIs and authorization

## Task Description

Move user, organization, role, permission, menu, and API authorization authority to Keycloak, consuming Task 3's stable identity and synchronization services.

- User create/update/disable and password reset operate on Keycloak Users; keep mapped OA user IDs and local projections for workflow/audit references. Local hashes must not be used as credentials.
- Department tree, hierarchy, leader/membership changes operate on Keycloak Groups; retain corresponding local numeric projection IDs.
- Application roles use Keycloak client roles with user/group role mappings. Menu and API grants use Keycloak Authorization Services resources/scopes/policies; route/menu metadata is stored as Keycloak resource attributes.
- Workbench publish/update registers or reconciles its menu and API resources idempotently.
- Enforce Keycloak-derived authorization on backend route handlers; frontend menu hiding alone is insufficient.
- Keep existing API contracts stable where possible; update DTOs to external IDs only where required, with OA numeric IDs retained for workflow data.

## Global Constraints (verbatim)

- Do not change tenant schema, tenant filtering, or tenant initialization semantics.
- Keep existing numeric OA user and department IDs stable for workflow/audit references.
- Keycloak is authoritative for credentials, users, groups, role assignments, and authorization policies.
- Never expose client secrets, service-account credentials, or access tokens to browser logs or public configuration responses.
- OA remains authoritative for workflow definitions, form schemas, instances, and history.

## Context

Backend repository `D:\workspace\oa-p\oa`, frontend repository `D:\workspace\oa-p\oa-web`; both are on `feature/keycloak-integration`. Read the design spec and current API/UI usage before changing contracts. Existing workflow data joins local `User`, `Department`, `DepartmentUser`, and audit IDs; do not remove those projections or mutate tenant semantics. Relevant APIs are `/user/**`, `/organize/**`, `/role/**`, `/permission/**`, and `/login/getMyMenus`. `WorkbenchService` owns publication behavior.

## Instructions

- Do not add or run tests. Non-test compile/build checks are allowed.
- Scope changes to Task 4; Task 5 owns admin UI changes and Task 6 owns historical workflow migration.
- Do not push. Commit the backend Task 4 work when complete.
- Self-review and write the detailed report to the path supplied by the controller.

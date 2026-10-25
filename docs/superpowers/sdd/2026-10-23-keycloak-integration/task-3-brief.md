# Task 3: Keycloak Admin REST client and stable identity links

## Task Description

Implement the plan's Task 3 against the current Keycloak setup and OIDC principal, without beginning Task 4 management API replacement.

- Implement service-account token caching keyed by current configuration and expiry; authenticated Admin REST operations must use the configured realm and must not log token or secret values.
- Persist stable, unique mappings between Keycloak user subjects/group IDs (and role IDs where already provisioned) and existing OA numeric IDs. Do not alter tenant fields/schema or change existing local IDs.
- Add idempotent synchronization primitives for users (create/update/enable/disable), hierarchical Keycloak groups and membership, and role mappings into existing workflow-compatible local projections.
- Disable local projections when linked Keycloak objects are disabled; never delete projections or mappings needed by workflow history.
- Resolve the authenticated OIDC subject to the mapped OA numeric user ID for JPA auditing/current-user behavior.

Keep Keycloak authoritative for user/group/role writes; local `User`, `Department`, `DepartmentUser`, and any existing role projection remain workflow compatibility data only. Preserve tenant initialization/filter semantics. Existing OAuth setup from Tasks 1¨C2 is the consumer contract. Do not rewrite the CRUD controllers yet; that belongs to Task 4.

## Global Constraints (verbatim)

- Do not change tenant schema, tenant filtering, or tenant initialization semantics.
- Keep existing numeric OA user and department IDs stable for workflow/audit references.
- Keycloak is authoritative for credentials, users, groups, role assignments, and authorization policies.
- Never expose client secrets, service-account credentials, or access tokens to browser logs or public configuration responses.
- OA remains authoritative for workflow definitions, form schemas, instances, and history.

## Context

Backend repository `D:\workspace\oa-p\oa`, frontend `D:\workspace\oa-p\oa-web`, both on `feature/keycloak-integration`. Read the design at `D:\workspace\oa-p\docs\superpowers\specs\2026-10-23-keycloak-integration-design.md`, current Admin client/link/principal code, and current workflow uses of numeric IDs. Workflow task assignment stores numeric OA user and department IDs; synchronization must preserve them. Do not change tenant schema, tenant filter behavior, or task assignment schema.

## Instructions

- Do not add or run tests. Non-test compile/build checks are allowed.
- Make focused changes only for Task 3; management API/controller replacement belongs to Task 4.
- Do not push. Commit this task in the backend repository when complete.
- Self-review and write the detailed report to the path supplied by the controller.

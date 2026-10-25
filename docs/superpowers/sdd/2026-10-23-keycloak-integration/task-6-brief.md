# Task 6: Workflow identity and existing-data migration

## Task Description

Preserve workflow instances/history and migrate existing local identity/authorization data into Keycloak idempotently.

- Provide an existing-installation bootstrap path for databases whose tenant initialization already completed. It must use the deployment-provided one-time setup secret, link the selected existing administrator's Keycloak `sub` to the existing OA user ID, and must never call tenant initialization or create a duplicate tenant.
- Import/link existing OA users, groups, roles, and menu/resource metadata into Keycloak. Re-running must reconcile rather than duplicate objects.
- Preserve all local user/department numeric IDs and retain disabled user projections for audit/history. Incompatible legacy password hashes are not copied; require Keycloak password reset/update action.
- Resolve workflow assignees through synchronized Keycloak group membership while retaining current numeric assignment references and candidate-group compatibility.
- Ensure workbench publish/update reconciles resources idempotently.
- Document migration order and rollback behavior in `docs/keycloak-migration.md`.

## Global Constraints (verbatim)

- Do not change tenant schema, tenant filtering, or tenant initialization semantics.
- Keep existing numeric OA user and department IDs stable for workflow/audit references.
- Keycloak is authoritative for credentials, users, groups, role assignments, and authorization policies.
- Never expose client secrets, service-account credentials, or access tokens to browser logs or public configuration responses.
- OA remains authoritative for workflow definitions, form schemas, instances, and history.

## Context

Backend `D:\workspace\oa-p\oa`; frontend `D:\workspace\oa-p\oa-web`; both on `feature/keycloak-integration`. Existing workflow queries use numeric user and department IDs: `FlowFormService`, `WorkflowService`, `FlowFormRepository`, and `UserTaskCreateListener`. Preserve those formats and the existing tenant listener/filter behavior. The current first-run endpoint rejects if tenant initialization already completed, so migration setup must be separate and must not call `InitService.init`.

## Instructions

- Do not add or run tests. Non-test compile/build checks are allowed.
- Scope changes to migration/workflow compatibility and operator migration docs.
- Do not push. Commit backend migration code and docs when complete.
- Self-review and write the detailed report to the path supplied by the controller.

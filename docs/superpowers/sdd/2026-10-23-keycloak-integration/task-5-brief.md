# Task 5: Initialization and administration UI

## Task Description

Finish the setup/login/navigation/user/organization/role/permission UI against the backend APIs from Tasks 1¨C4.

- First-run Keycloak wizard collects and validates issuer, realm, OAuth client, service-account client, callback/logout URLs, required setup gate input, and shows only secret-free connection status. The app client and service account secrets are never logged or returned.
- Login uses Keycloak redirect and displays the current OIDC-backed user; logout ends both OA and Keycloak sessions. Axios uses same-origin cookies and CSRF.
- User/org/role/permission screens use Keycloak-backed DTOs and IDs, with local numeric references only where workflow APIs require them.
- Render only menus authorized for the current principal; backend authorization remains authoritative.
- Remove all tenant fields, filters, labels, and tenant selection UI; keep only the existing tenant-related backend/data behavior required by global constraints.

## Global Constraints (verbatim)

- Do not change tenant schema, tenant filtering, or tenant initialization semantics.
- Keep existing numeric OA user and department IDs stable for workflow/audit references.
- Keycloak is authoritative for credentials, users, groups, role assignments, and authorization policies.
- Never expose client secrets, service-account credentials, or access tokens to browser logs or public configuration responses.
- OA remains authoritative for workflow definitions, form schemas, instances, and history.

## Context

Frontend repository `D:\workspace\oa-p\oa-web`, backend repository `D:\workspace\oa-p\oa`; both on `feature/keycloak-integration`. Existing route pages are `src/app/init/page.tsx`, `src/app/app/login/{Login,Menu}.tsx`, `src/app/app/usermanage/page.tsx`, `organizes/page.tsx`, `roles/page.tsx`, and `permissions/page.tsx`. `service.ts` proxies API calls through `/server`.

## Instructions

- Do not add or run tests. Non-test build/type checks are allowed.
- Scope changes to frontend work; Task 4 owns backend contract changes.
- Do not push. Commit frontend work when complete.
- Self-review and write the detailed report to the path supplied by the controller.

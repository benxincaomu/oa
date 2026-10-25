# Keycloak Authoritative Directory and Authorization Implementation Plan

> **For agentic workers:** Execute inline using `superpowers:executing-plans`, task by task, preserving existing uncommitted work.

**Goal:** Make Keycloak the only source for OA user, organization, role, and permission data while preserving existing workflow history and tenant behavior.

**Architecture:** OA authenticates users through Keycloak OIDC and retrieves current identity and authorization data from Keycloak APIs through backend endpoints. OA retains workflow business data and only the opaque Keycloak-to-legacy-ID links required to resolve historical numeric workflow references. Keycloak Authorization Services stores baseline and dynamically published workflow resources, scopes, policies, and permissions.

**Tech Stack:** Java 21, Spring Boot 3.5.4, Spring Security OAuth2 Client, PostgreSQL/JPA, Redis where already needed, Next.js 15, React 19, Ant Design, Keycloak Admin REST API and UMA 2.0.

**Spec:** `docs/superpowers/specs/2026-10-23-keycloak-integration-design.md` plus the user-approved decisions in this conversation.

## Global Constraints

- Keep existing tenant isolation and workflow business data behavior.
- Do not read current user, group, role, or permission profiles from OA identity tables; obtain them from Keycloak APIs.
- Retain only opaque identity links required to resolve legacy numeric references; do not use them as a directory cache.
- Keep Keycloak service-account credentials and Admin REST calls on the backend.
- Keycloak is authoritative for users, groups, application roles, and application authorization policy.
- Workbench definitions, workflow instances, task history, and business-level task ownership remain OA data.
- Do not add or run tests; use code review and non-test compilation/build checks only.

---

### Task 1: Add an idempotent Keycloak Authorization Services layer

**Files:**
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/security/KeycloakAdminClient.java`
- Create: `oa/src/main/java/io/github/benxincaomu/oa/base/security/KeycloakAuthorizationService.java`
- Create: authorization DTOs under `oa/src/main/java/io/github/benxincaomu/oa/base/security`

**Deliverable:** OA can idempotently create, update, list, and delete its Keycloak authorization resources and scopes, and link role policies to them using stable application-owned keys.

- [ ] Define stable resource keys and attributes for menu resources, API resources, tenant IDs, workbench keys, display names, parent keys, and sort order.
- [ ] Add typed Admin Client operations for resource and scope lookup/upsert/delete; keep raw Keycloak payload handling inside the client.
- [ ] Add policy and permission operations for application client roles; validate that referenced roles belong to the configured OA client.
- [ ] Make retries safe by looking up resources using stable names/attributes before creating them.
- [ ] Keep Keycloak-specific Admin REST representations behind `KeycloakAuthorizationService` interfaces consumed by initialization and workbench publication.

### Task 2: Provision baseline roles and permissions during initialization

**Files:**
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/init/InitController.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/init/InitService.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/security/KeycloakAdminClient.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/security/KeycloakBootstrapIdentity.java`

**Deliverable:** First-run setup creates the initial administrator, tenant root group, application roles, static menu/API resources, and initial role policies in Keycloak. OA stores tenant data and required opaque identity links only; it does not seed local role or permission grants.

- [ ] Define the baseline OA role and resource catalog in code with stable keys and tenant ownership.
- [ ] Create or resolve the OA application client role for the initial administrator and assign it in Keycloak; avoid using a parallel realm role as the OA role source.
- [ ] Provision baseline menu/API resources and policies through `KeycloakAuthorizationService` before initialization is marked complete.
- [ ] Preserve tenant bootstrap and store only subject/group/legacy-ID links required by workflow compatibility.
- [ ] Make setup idempotent across partial failures and prevent an incomplete Keycloak setup from appearing initialized.

### Task 3: Make identity APIs read Keycloak without writing directory projections

**Files:**
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/security/IdentityManagementController.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/security/KeycloakDirectorySyncService.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/security/KeycloakAdminClient.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/security/OaOidcUserService.java`
- Modify: legacy controllers under `oa/src/main/java/io/github/benxincaomu/oa/bussiness/user` and `.../organization`

**Deliverable:** Management reads return Keycloak profiles directly, with Keycloak-side search/pagination and tenant scoping. Login principal identity comes from the OIDC subject and does not require a local User row.

- [ ] Scope users and groups to the current tenant using Keycloak group hierarchy/attributes rather than local User or Department rows.
- [ ] Stop calling `projectUserProfile`, `projectGroupProfile`, or role/membership projection methods on read paths.
- [ ] Add Keycloak-backed role, group-membership, and authorization-resource reads to the backend API.
- [ ] Keep existing write endpoints Keycloak-backed and validate tenant ownership before mutation.
- [ ] Disable or delegate legacy `/user`, `/organize`, `/role`, and `/permission` management routes so they cannot continue writing local identity/authorization data.
- [ ] Keep the OIDC subject on the authenticated principal; use opaque links only when translating legacy numeric workflow references.

### Task 4: Register dynamic workbench permissions in Keycloak

**Files:**
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/bussiness/workflow_design/WorkbenchService.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/bussiness/workflow_design/WorkbenchPublishService.java` if publication state is needed
- Create: a retryable authorization reconciliation record/worker only if required by the existing transaction model

**Deliverable:** Every workbench publish/update registers or reconciles its menu and API resources in Keycloak; stale resources are removed when a workbench is retired.

- [ ] Derive stable resource keys from tenant ID, workbench key, and resource purpose rather than generated Keycloak IDs.
- [ ] Register menu metadata and API paths/scopes through `KeycloakAuthorizationService` on every publication, not only the first version.
- [ ] Preserve the current default administrator access when creating new workbench resources.
- [ ] Ensure authorization registration failure is visible and retryable; do not report a fully active publication until its authorization resources exist.
- [ ] Remove the local `PermissionService.insert` calls from workbench publication.

### Task 5: Use Keycloak authorization for menus and protected API routes

**Files:**
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/security/SecurityConfig.java`
- Create: `oa/src/main/java/io/github/benxincaomu/oa/base/security/KeycloakAuthorizationManager.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/bussiness/user/OaLoginController.java`
- Modify: protected controllers and route configuration

**Deliverable:** Menus are generated from Keycloak-authorized resources, and backend requests require Keycloak authorization for the matching resource and scope. Workflow task ownership checks remain in OA.

- [ ] Retrieve the authenticated user's Keycloak access token from the authorized OIDC client and request UMA authorization for the current resource/scope.
- [ ] Fail closed when a protected route has no registered Keycloak resource/scope or the authorization service cannot grant access.
- [ ] Replace local `RoleRepository`/`PermissionService` menu construction with Keycloak-authorized resource metadata.
- [ ] Keep coarse administrative authorities for administrative API groups and enforce fine-grained authorization on dynamic application routes.
- [ ] Do not use frontend menu hiding as the only access control.

### Task 6: Switch administration UI to the Keycloak-backed API

**Files:**
- Modify: `oa-web/src/app/app/usermanage/page.tsx`
- Modify: `oa-web/src/app/app/organizes/page.tsx`
- Modify: `oa-web/src/app/app/roles/page.tsx`
- Modify: `oa-web/src/app/app/permissions/page.tsx`
- Modify: `oa-web/src/app/app/login/Menu.tsx`
- Modify: frontend API DTOs and calls under `oa-web/src`

**Deliverable:** User, organization, role, permission, and menu views display Keycloak data and submit changes through OA's Keycloak-backed endpoints.

- [ ] Replace legacy `/user`, `/organize`, `/role`, and `/permission` calls with the stabilized Keycloak-backed API contracts.
- [ ] Use Keycloak string IDs in UI state and mutation requests; avoid local numeric role/permission IDs.
- [ ] Render menus from the Keycloak-authorized menu response.
- [ ] Remove local-only permission CRUD and role-permission assignment behavior.
- [ ] Keep service-account credentials and Keycloak Admin REST calls out of browser code.

### Task 7: Preserve workflow references and resolve current identity data through Keycloak

**Files:**
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/entity/JpaAuditorAware.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/bussiness/workflow/FlowFormService.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/bussiness/workflow/WorkflowService.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/bussiness/workflow/FlowHistoryRepository.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/bussiness/workflow/FlowFormRepository.java`
- Modify: identity-link repository and migration documentation

**Deliverable:** Existing workflow/audit references continue resolving while current profile, group membership, and display information are obtained from Keycloak APIs.

- [ ] Inventory current numeric user/group references in workflow tables, dynamic tables, candidate groups, and audit rows.
- [ ] Preserve immutable subject/group-to-legacy-ID links for existing rows; do not use User/Department/Role records as current profile sources.
- [ ] Resolve current user and group membership through Keycloak APIs, translating to legacy IDs only at existing workflow query boundaries.
- [ ] Store historical operator display snapshots in workflow history where needed so audit records remain readable if a Keycloak user is later disabled.
- [ ] Document an idempotent mapping backfill and rollback path before retiring local directory projections.

### Task 8: Review and verify the cutover

**Files:**
- Review affected backend and frontend files.
- Modify: `docs/keycloak-migration.md`

**Deliverable:** Operators can initialize Keycloak, manage identity and authorization there, publish protected workbenches, and run existing workflows without local identity/permission tables serving current data.

- [ ] Review tenant scoping, service-account permissions, fail-closed authorization, and secret handling.
- [ ] Confirm all current identity/organization/role/permission reads go through Keycloak-backed services.
- [ ] Run `mvn -q -DskipTests compile` in `oa` and the frontend production build in `oa-web`; do not add or run tests.
- [ ] Run `git diff --check` in both repositories and review all changed files, including pre-existing uncommitted changes.

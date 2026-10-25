# Keycloak Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Delegate OA authentication, users, organizations, roles, and permissions to Keycloak while preserving workflow compatibility and tenant behavior.

**Architecture:** Spring Security handles OAuth2 authorization-code login and maintains a secure server-side session. A backend Keycloak Admin REST client manages Keycloak users, groups, roles, and authorization resources. OA keeps workflow-compatible local identity links/projections and continues to own workflow business data.

**Tech Stack:** Java 21, Spring Boot 3.5.4, Spring Security OAuth2 Client, PostgreSQL/JPA, Redis where already needed, Next.js 15, React 19, Ant Design, Keycloak Admin REST API.

**Spec:** `docs/superpowers/specs/2026-10-23-keycloak-integration-design.md`

## Global Constraints

- Do not change tenant schema, tenant filtering, or tenant initialization semantics.
- Keep existing numeric OA user and department IDs stable for workflow/audit references.
- Keycloak is authoritative for credentials, users, groups, role assignments, and authorization policies.
- Never expose client secrets, service-account credentials, or access tokens to browser logs or public configuration responses.
- OA remains authoritative for workflow definitions, form schemas, instances, and history.

---

### Task 1: Store and validate Keycloak connection settings

**Files:**
- Create: `oa/src/main/java/io/github/benxincaomu/oa/base/security/keycloak/KeycloakIntegrationConfig.java`
- Create: `oa/src/main/java/io/github/benxincaomu/oa/base/security/keycloak/KeycloakIntegrationConfigRepository.java`
- Create: `oa/src/main/java/io/github/benxincaomu/oa/base/security/keycloak/KeycloakSecretCipher.java`
- Create: `oa/src/main/java/io/github/benxincaomu/oa/base/security/keycloak/KeycloakConnectionService.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/init/InitController.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/init/InitService.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/init/vo/InitVo.java`
- Modify: `oa/src/main/resources/application.yml`
- Modify: `oa/pom.xml`

**Deliverable:** First-run setup accepts issuer, realm, OIDC client, service-account client, and callback settings; the server validates OIDC discovery and Admin API access, encrypts secrets with a deployment key, and exposes only a secret-free public status DTO.

- [ ] Add Spring Security OAuth2 client and required crypto dependencies.
- [ ] Add a JPA config entity for connection settings and encrypted secrets; never serialize this entity from a controller.
- [ ] Add AES-GCM encryption/decryption using a required environment-supplied master key; fail setup if the key is missing or invalid.
- [ ] Validate issuer discovery and retrieve a service-account token before saving configuration.
- [ ] Keep the existing tenant bootstrap code and resulting tenant records unchanged.

### Task 2: Replace local token login with OIDC session authentication

**Files:**
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/security/SecurityConfig.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/security/SecurityFilter.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/security/SecurityConsts.java`
- Create: `oa/src/main/java/io/github/benxincaomu/oa/base/security/keycloak/KeycloakClientRegistrationRepository.java`
- Create: `oa/src/main/java/io/github/benxincaomu/oa/base/security/keycloak/KeycloakAuthoritiesMapper.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/bussiness/user/LoginController.java`
- Modify: `oa-web/src/app/app/login/Login.tsx`
- Modify: `oa-web/src/commons/base/service.ts`

**Deliverable:** `/oauth2/authorization/keycloak` starts login, the OIDC callback creates a Spring session, API calls use the HttpOnly session cookie, and logout also ends the Keycloak SSO session.

- [ ] Resolve the `keycloak` client registration from persisted issuer/client settings, caching discovery metadata until configuration changes.
- [ ] Configure OAuth2 login, OIDC user loading, secure session-cookie settings, and Keycloak logout redirect.
- [ ] Remove the custom Redis token filter from the active security chain while preserving public initialization and health endpoints.
- [ ] Map Keycloak realm/client roles to Spring authorities.
- [ ] Replace the username/password modal with Keycloak login redirect, current-user display, and logout.
- [ ] Ensure Axios sends same-origin cookies and never reads an access token from JavaScript.

### Task 3: Add Keycloak Admin REST client and stable identity links

**Files:**
- Create: `oa/src/main/java/io/github/benxincaomu/oa/base/security/keycloak/KeycloakAdminClient.java`
- Create: `oa/src/main/java/io/github/benxincaomu/oa/base/security/keycloak/KeycloakObjectLink.java`
- Create: `oa/src/main/java/io/github/benxincaomu/oa/base/security/keycloak/KeycloakObjectLinkRepository.java`
- Create: `oa/src/main/java/io/github/benxincaomu/oa/base/security/keycloak/KeycloakDirectorySyncService.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/base/entity/JpaAuditorAware.java`
- Modify: services and repositories under `oa/src/main/java/io/github/benxincaomu/oa/bussiness/user` and `.../organization`

**Deliverable:** OA manages and reconciles Keycloak users and hierarchical groups while preserving local numeric IDs required by workflows.

- [ ] Implement service-account token caching by expiry and authenticated Admin REST calls.
- [ ] Add unique external-subject/group-ID links without changing tenant fields or constraints.
- [ ] Synchronize user create/update/enable/disable, group hierarchy/membership, and role mappings into workflow projections.
- [ ] Deactivate rather than delete linked local projections so historical records still resolve.
- [ ] Resolve the current auditor from the OIDC subject through the stable local-ID mapping.

### Task 4: Move management APIs and authorization decisions to Keycloak

**Files:**
- Modify: user controllers/services/DTOs under `oa/src/main/java/io/github/benxincaomu/oa/bussiness/user`
- Modify: department controllers/services/DTOs under `oa/src/main/java/io/github/benxincaomu/oa/bussiness/organization`
- Modify: role and permission controllers/services under `oa/src/main/java/io/github/benxincaomu/oa/bussiness/user`
- Create: Keycloak management DTOs under `oa/src/main/java/io/github/benxincaomu/oa/base/security/keycloak`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/bussiness/workflow_design/WorkbenchService.java`

**Deliverable:** Existing admin APIs read/write Keycloak; backend API authorization and menu visibility derive from Keycloak roles/resources/policies.

- [ ] Replace local user CRUD and password reset with Keycloak operations; preserve mapped OA IDs.
- [ ] Replace department tree and membership CRUD with hierarchical Keycloak Groups.
- [ ] Replace local role grants with Keycloak client roles and user/group role mappings.
- [ ] Move app menu/API grants to Keycloak client roles and Authorization Services resources/scopes/policies; store route/menu metadata as Keycloak resource attributes.
- [ ] Register workbench-specific menu/API resources in Keycloak as part of publication.
- [ ] Enforce Keycloak-derived authorities on backend routes; do not rely on frontend menu visibility.

### Task 5: Update initialization and administration UI

**Files:**
- Modify: `oa-web/src/app/init/page.tsx`
- Modify: `oa-web/src/app/app/login/Login.tsx`
- Modify: `oa-web/src/app/app/login/Menu.tsx`
- Modify: `oa-web/src/app/app/usermanage/page.tsx`
- Modify: `oa-web/src/app/app/organizes/page.tsx`
- Modify: `oa-web/src/app/app/roles/page.tsx`
- Modify: `oa-web/src/app/app/permissions/page.tsx`
- Modify: `oa-web/src/commons/base/service.ts`

**Deliverable:** Setup validates Keycloak; identity/admin pages display Keycloak-backed data; navigation reflects Keycloak authorization; UI has no tenant concepts.

- [ ] Replace local password setup with Keycloak connection fields and secret-free connection status.
- [ ] Validate issuer/realm, application client, service-account client, and callback values before submit.
- [ ] Update user/org/role/permission UI DTOs to Keycloak identifiers and attributes.
- [ ] Render only menu resources authorized for the current principal.
- [ ] Remove tenant fields, filters, labels, and tenant-selection interactions from UI flows.

### Task 6: Preserve workflow identity and migrate existing authorization data

**Files:**
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/bussiness/workflow/FlowFormService.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/bussiness/workflow/WorkflowService.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/bussiness/workflow/FlowFormRepository.java`
- Modify: `oa/src/main/java/io/github/benxincaomu/oa/bussiness/workflow_design/WorkbenchService.java`
- Create: `docs/keycloak-migration.md`

**Deliverable:** Existing flow instances, audit history, and local numeric identity references remain valid under Keycloak authorization.

- [ ] Build an idempotent migration linking or importing OA users/groups/roles into Keycloak without changing tenant data.
- [ ] Preserve local user/department IDs and retain disabled users for audit/history views.
- [ ] Resolve workflow assignees from synchronized Keycloak group membership while retaining stored flow references.
- [ ] Ensure workbench publish/update reconciles its Keycloak resources idempotently.
- [ ] Document rollback and password-reset behavior for incompatible legacy hashes.

### Task 7: Final review and operator documentation

**Files:**
- Review changes across `oa` and `oa-web`.
- Modify: `oa/README.md`
- Modify: `oa-web/README.md`

**Deliverable:** Operators can configure Keycloak, complete first-run setup, authenticate, manage identity/authorization, and run existing workflows without tenant UI changes.

- [ ] Review public endpoints, authority mappings, secret handling, proxy/cookie behavior, and logout redirects.
- [ ] Confirm tenant entities, listeners, filters, and bootstrap semantics remain unchanged.
- [ ] Confirm both repositories remain on `feature/keycloak-integration` and no unrelated files are staged.


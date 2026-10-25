# SDD ledger - plan: D:\workspace\oa-p\docs\superpowers\plans\2026-10-23-keycloak-integration.md

## Workspace and rulings

Ruling: Work in the user-requested feature/keycloak-integration branch in each existing repository rather than creating linked worktrees. The user explicitly requested those development branches and both are off the default branch. A second checkout would conflict with the already checked-out branch names. Cost if wrong: less filesystem isolation than linked worktrees, while commits remain isolated on the requested branches.

Ruling: Do not add or run tests due to the developer instruction; use code review and non-test build/compile checks where available. Cost if wrong: runtime regressions may be detected later than with tests.

## Preflight dependency scan

| Tasks | Shared file/interface | Producer -> consumer | Finding |
|---|---|---|---|
| 1 -> 2 | InitController / persisted Keycloak configuration | Task 1 persists and validates settings -> Task 2 resolves OIDC registration from those settings | Compatible; config lookup must be stable before OIDC registration consumes it. |
| 1 -> 3 | InitService / Keycloak configuration | Task 1 bootstraps configured identity -> Task 3 provides durable identity links/admin operations | Compatible; bootstrap links are the foundation Task 3 completes. |
| 2 -> 3 | OIDC principal / JpaAuditorAware | Task 2 authenticates Keycloak subject -> Task 3 resolves its stable OA user ID | Compatible; mapping is required before OIDC principals can audit workflows. |
| 2 -> 5 | login page / HTTP client | Task 2 introduces OIDC redirect and session cookies -> Task 5 finalizes login UI and service behavior | Compatible; Task 5 builds on the session contract. |
| 3 -> 4 | Keycloak Admin client and identity projections | Task 3 supplies user/group sync operations -> Task 4 consumes them for management APIs | Compatible; execute sequentially. |
| 4 -> 5 | user/org/role/permission API contracts | Task 4 changes source and DTOs -> Task 5 renders Keycloak-backed DTOs | Compatible; backend contracts settle first. |
| 4 -> 6 | WorkbenchService | Task 4 protects/registers app resources -> Task 6 reconciles workbench resources idempotently | Compatible; merge into one publication flow. |
| 3 -> 6 | workflow assignee projections | Task 3 synchronizes identity/group membership -> Task 6 resolves workflow assignees | Compatible; preserve numeric references. |
| 1 | InitController, InitService, InitVo, application.yml, pom.xml | Validate/encrypt/store config; preserve tenant bootstrap | Agrees with itself and tenant constraint. |
| 2 | SecurityConfig, SecurityFilter, SecurityConsts, login APIs/UI, service.ts | OIDC session and CSRF contract | Agrees with itself; consumes Task 1 config. |
| 3 | Admin client, link entity/repository, directory sync, auditor | External IDs resolve to existing OA IDs | Agrees with itself; operations expanded in Task 4. |
| 4 | user/org/role/permission APIs, Keycloak DTOs, WorkbenchService | Keycloak authority source and backend enforcement | Agrees with itself; Task 6 adds publication reconciliation. |
| 5 | init and administration UI | Reflect Task 1/2 and Task 4 API contracts; hide tenant concepts | Agrees with itself; schema and logic remain untouched. |
| 6 | workflow services/repository, WorkbenchService, migration docs | Keep historic IDs and idempotent migration/resource sync | Agrees with itself; WorkbenchService shared with Task 4. |
| 7 | README files and whole branch | Operator setup, review, tenant behavior unchanged | Agrees with itself. |

## Progress

- [x] Tasks 1-2: Keycloak settings, OIDC sessions, setup/login UI
- [x] Task 3: Admin REST client and stable identity links
- [ ] Task 4: Keycloak management APIs and authorization
- [ ] Task 5: Initialization and administration UI
- [ ] Task 6: Workflow identity and existing-data migration
- [ ] Task 7: Final review and operator documentation

### Existing-install initialization gap

InitController.init rejects when InitService.isInitialized() is true, while OIDC login requires the subject-to-local-user link and Task 6 requires migrating existing local rows. A legacy installation with tenant data therefore cannot reach Keycloak setup/migration through the first-run endpoint.

Ruling: Add a distinct one-time existing-installation Keycloak bootstrap path gated by a deployment-provided setup secret, separate from tenant initialization. It must establish the first linked administrator and must never call tenant initialization. This preserves tenant records and init semantics while enabling migration. Cost if wrong: operators provision and submit one additional setup secret; a weaker gate would permit unauthorized identity-provider takeover.

### Public initialization endpoint security gap

The first-run initialization endpoint is public and fetches OIDC discovery/token URLs supplied in the request. Without a deployment-controlled gate, this creates SSRF exposure and lets an unauthenticated caller race to set the initial identity provider.

Ruling: Require a deployment-provided, one-time Keycloak setup token for first-run Keycloak configuration; compare it in constant time, never return/log it, and make it unusable after successful setup. This is the smallest reliable gate while allowing operators to enter arbitrary valid Keycloak hosts. Cost if wrong: initial deployment gains one environment secret and an extra wizard field; omitting the gate leaves IdP takeover and SSRF paths.

### Tasks 1-2 review findings and ruling

Tasks 1-2 were implemented in backend commits 44b9a52..a5c54be and frontend commits 99adee7..4f7b28c. Backend compile and frontend production build passed; no tests were added or run.

The reviewer found that Keycloak roles are mapped but local OA role/permission data still drives getMyMenus, and the request chain only requires authentication.

Ruling: Treat role-to-authority mapping as Task 2's deliverable and menu/API enforcement as Task 4's deliverable. Task 4 must replace remaining local menu grants and enforce Keycloak-derived authorities before the overall plan can be considered complete. Cost if wrong: local role grants could remain an authorization source after integration.

The reviewer also found /actuator/beans exposed publicly beyond the specified init/login/health endpoints. Backend commit 44a05c5 removes it from both public allowlists; scoped re-review confirmed it addressed with no new breakage. Verification: mvn -q -DskipTests compile passed; no tests were added or run.

Task 1-2: complete (backend commits 44b9a52..44a05c5, frontend commits 99adee7..4f7b28c; review clean in scope). The local-menu authorization gap remains assigned to Task 4 and blocks overall plan completion until Keycloak-derived access is enforced.


Task 3 implementation: commit c20966a (feat: add Keycloak directory synchronization); mvn -q -DskipTests compile passed; git diff --check passed; no tests were added or run. Task review pending.

Task 3: complete (backend commits c20966a, b0069d1, a1cb98e, 427d53b; reviewer findings addressed in scoped re-reviews). It synchronizes Keycloak users/groups/members/roles to stable OA projections, rejects null or mismatched tenant IDs before external writes, rechecks natural-key matches before upsert, and paginates group lookup. `mvn -q -DskipTests compile` and `git diff --check` passed; no tests were added or run.

The scoped re-review then found that explicit IDs made expected-ID checks tautological, and Keycloak group search was not paginated. Commit 427d53b now rechecks the username or parent/name match inside the Admin client and scans group pages with `first`/`max`; non-test compile and `git diff --check` passed. Scoped re-review approved both fixes and found no new issues.

Generic protocol/algorithm naming refactor was committed separately as b888170 (`refactor: name generic OIDC components by protocol`).

Ruling: Treat linked users, departments, and roles with a null tenant ID as invalid for directory synchronization; initialization and tenant assignment give these projections a concrete tenant, and accepting null would allow an unscoped object to be updated in any tenant context. Cost if wrong: intentionally global directory projections will need an explicit global model before synchronization can support them.

Ruling: Use protocol/algorithm names for generic OIDC and AES-GCM components (`OidcClientRegistrationRepository`, `OaOidcUserService`, `OidcLogoutSuccessHandler`, `AesGcmSecretCipher`); retain `Keycloak` in classes bound to its Admin REST API, Realm, claims, Groups, and linked-object schema. Keep the existing OAuth registration ID `keycloak` to preserve saved callback URIs. Cost if wrong: class names may still understate some Keycloak-specific dependencies or the callback path will retain the provider label for compatibility.

Task 4 was initially dispatched from backend base c20966a and paused before implementation when Task 3 review findings surfaced. Task 3 review is now clean; resume Task 4 from backend head 427d53b on `feature/keycloak-integration`. Frontend Task 5 inventory confirmed both frontend branch and working tree are clean; no explicit tenant labels or identifiers were found under `oa-web/src` or its README, but the initialization and identity administration screens still use the local OA user/role/permission contracts and need conversion after Task 4 stabilizes.

Ruling: Enforce backend route access by requesting UMA permissions from Keycloak Authorization Services with the authenticated user's OIDC access token; fail closed if a route has no registered resource/scope or no granted permission. Provision menu/API resources and role policies through the Admin API, keeping local permission/role rows only as numeric-ID projections. Cost if wrong: each uncached authorization decision adds a Keycloak round trip, and deployments must enable Authorization Services on the application client.

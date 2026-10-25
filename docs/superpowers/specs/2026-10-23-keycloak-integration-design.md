# Keycloak Integration Design

## Goal

Make Keycloak the authoritative platform for OA authentication, user lifecycle, organizational groups, roles, and permissions, while preserving OA workflow compatibility and all existing tenant schema and behavior.

## Architecture

The browser starts OAuth2/OIDC authorization-code login through the existing `/server` proxy. Spring Security completes the OIDC flow and keeps the authenticated session in a secure, HttpOnly cookie. The backend uses a confidential service client for Keycloak Admin REST calls; credentials are encrypted at rest and never sent back to the browser.

Keycloak is authoritative for users, groups, role assignments, application client roles, and authorization resources/policies. OA retains workflow business data and stable identity links/projections needed by existing tables that refer to numeric OA user and department IDs. Existing tenant tables, tenant filters, and tenant bootstrap behavior remain unchanged; tenant concepts are not shown in the UI.

## Ownership

- Authentication, credentials, SSO, session, and logout: Keycloak OIDC.
- Users and identity attributes: Keycloak Users; OA retains a Keycloak subject to local numeric ID link.
- Department hierarchy and membership: Keycloak hierarchical Groups; OA retains group ID links/projections for existing task assignment queries.
- Roles and grants: Keycloak client roles and user/group role mappings.
- Application menu/API permissions: Keycloak roles and Authorization Services resources/scopes/policies. OA registers workbench-specific resources with Keycloak when a workbench is published.
- Workflow definitions, dynamic form schemas, workflow instances, audit history, and workflow notifications: OA, protected by Keycloak-derived authorities.
- Tenant schema and tenant-scoped behavior: unchanged.

## Initialization

The first-run wizard accepts the Keycloak issuer/realm, OAuth2 application client configuration, backend service-account client credentials, and callback/base URL settings. It validates OIDC discovery and service-account access before marking the project initialized. Secrets are encrypted using a deployment-provided master key and are not returned by configuration APIs or written to logs. Existing tenant bootstrap runs unchanged; the initial OA administrator is linked to a Keycloak identity rather than given an OA-managed password.

## Identity mapping and migration

Existing OA numeric user and department IDs remain stable so flow history, creator fields, task assignments, and foreign references keep resolving. A durable mapping relates Keycloak `sub` and group IDs to OA IDs. Keycloak is the write authority; OA projections are refreshed by management operations and reconciliation. Existing password hashes are not copied unless Keycloak supports their exact format; otherwise users are linked and complete a Keycloak password reset.

## Security and failure behavior

- Public endpoints are limited to initialization, OIDC login/callback/logout, and health checks.
- Invalid or expired OIDC sessions are rejected and sent through Keycloak login again.
- Management APIs require Keycloak-derived administrative roles; hiding menu items is not authorization.
- Keycloak outages block identity and authorization writes with a clear error; they do not silently fall back to local grants.
- Encrypted secrets are not included in DTOs, logs, or frontend state.

## Constraints

- Do not change tenant tables, tenant filters, or tenant bootstrap semantics.
- Do not retain OA local passwords or local role grants as a parallel source of truth.
- Preserve existing numeric OA IDs and workflow audit/history data.
- Keep application workflow and form data in OA.


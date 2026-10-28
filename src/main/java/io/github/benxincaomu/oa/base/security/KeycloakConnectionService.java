package io.github.benxincaomu.oa.base.security;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provides the Keycloak settings loaded from application configuration and the
 * connection/validation helpers needed by the OIDC login and Admin REST clients.
 *
 * <p>
 * Configuration is not persisted in the database any more. It comes from
 * {@code app.keycloak.*}; secrets live in memory only and are never returned by
 * status endpoints or written to logs.
 * </p>
 */
@Service
public class KeycloakConnectionService {
    private final KeycloakIntegrationConfig configuration;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public KeycloakConnectionService(KeycloakIntegrationConfig configuration,
            RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.configuration = configuration;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public Optional<KeycloakIntegrationConfig> getConfiguration() {
        return configuration.isConfigured() ? Optional.of(configuration) : Optional.empty();
    }

    public KeycloakConnectionStatusVo getStatus() {
        return getConfiguration()
                .map(config -> new KeycloakConnectionStatusVo(true, config.getIssuerUri(), config.getRealm(),
                        config.getClientId(), config.getRedirectUri(), config.getPostLogoutRedirectUri()))
                .orElseGet(() -> new KeycloakConnectionStatusVo(false, null, null, null, null, null));
    }

    public String getClientSecret(KeycloakIntegrationConfig config) {
        return config.getClientSecret();
    }

    public String getServiceAccountClientSecret(KeycloakIntegrationConfig config) {
        return config.getServiceAccountClientSecret();
    }

    public String getTokenEndpoint(KeycloakIntegrationConfig config) {
        URI issuer = URI.create(config.getIssuerUri());
        URI discoveryUri = URI.create(trimTrailingSlash(config.getIssuerUri()) + "/.well-known/openid-configuration");
        JsonNode metadata;
        try {
            metadata = restClient.get().uri(discoveryUri).retrieve().body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("无法读取 Keycloak OIDC Discovery 配置", exception);
        }
        String tokenEndpoint = requiredMetadata(metadata, "token_endpoint");
        requireSameOrigin(issuer, tokenEndpoint, "token_endpoint");
        return tokenEndpoint;
    }

    /**
     * Validates the configured issuer, discovery document, callback URIs and
     * service-account access before tenant initialization touches Keycloak.
     */
    public void validateConfiguration() {
        KeycloakIntegrationConfig config = getConfiguration()
                .orElseThrow(() -> new IllegalStateException(
                        "Keycloak 尚未配置，请在 application.yml 或环境变量中设置 app.keycloak.*"));
        URI issuer = validateIssuer(config.getIssuerUri(), config.getRealm());
        URI discoveryUri = URI.create(trimTrailingSlash(issuer.toString()) + "/.well-known/openid-configuration");
        JsonNode metadata;
        try {
            metadata = restClient.get().uri(discoveryUri).retrieve().body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new IllegalArgumentException("无法读取 Keycloak OIDC Discovery 配置", exception);
        }
        if (metadata == null || !sameIssuer(issuer.toString(), metadata.path("issuer").asText())) {
            throw new IllegalArgumentException("Keycloak Discovery 返回的 issuer 与配置不匹配");
        }

        String tokenEndpoint = requiredMetadata(metadata, "token_endpoint");
        String authorizationEndpoint = requiredMetadata(metadata, "authorization_endpoint");
        requireSameOrigin(issuer, tokenEndpoint, "token_endpoint");
        requireSameOrigin(issuer, authorizationEndpoint, "authorization_endpoint");
        validateCallbackUri(config.getRedirectUri(), "OAuth2 回调地址");
        validateCallbackUri(config.getPostLogoutRedirectUri(), "退出登录回调地址");
        validateClientCredentials(tokenEndpoint, config);
    }

    private void validateClientCredentials(String tokenEndpoint, KeycloakIntegrationConfig config) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", config.getServiceAccountClientId().trim());
        form.add("client_secret", config.getServiceAccountClientSecret());

        JsonNode token;
        try {
            token = restClient.post()
                    .uri(tokenEndpoint)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException exception) {
            throw new IllegalArgumentException("Keycloak 管理客户端凭证无效或无法连接", exception);
        }
        String accessToken = token == null ? "" : token.path("access_token").asText();
        if (accessToken.isBlank()) {
            throw new IllegalArgumentException("Keycloak 管理客户端没有获得 access token");
        }
        validateAdminRoles(accessToken);

        URI adminRealmUri = adminRealmUri(config.getIssuerUri(), config.getRealm());
        try {
            restClient.get()
                    .uri(adminRealmUri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new IllegalArgumentException("Keycloak 管理客户端缺少访问 Realm 的权限", exception);
        }
    }

    private void validateAdminRoles(String accessToken) {
        String[] segments = accessToken.split("\\.");
        if (segments.length != 3) {
            throw new IllegalArgumentException("Keycloak service-account token is not a JWT");
        }
        try {
            JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(segments[1]));
            JsonNode roles = claims.path("resource_access").path("realm-management").path("roles");
            boolean fullAdmin = false;
            boolean manageUsers = false;
            boolean manageClients = false;
            boolean manageAuthorization = false;
            if (roles.isArray()) {
                for (JsonNode role : roles) {
                    String name = role.asText();
                    fullAdmin |= "realm-admin".equals(name);
                    manageUsers |= "manage-users".equals(name);
                    manageClients |= "manage-clients".equals(name);
                    manageAuthorization |= "manage-authorization".equals(name);
                }
            }
            if (!fullAdmin && !(manageUsers && manageClients && manageAuthorization)) {
                throw new IllegalArgumentException(
                        "Keycloak service account requires realm-admin or manage-users, manage-clients, and manage-authorization roles");
            }
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Keycloak service-account token claims could not be read", exception);
        }
    }

    private URI validateIssuer(String issuerValue, String realmValue) {
        if (issuerValue == null || realmValue == null || realmValue.isBlank()) {
            throw new IllegalArgumentException("Keycloak issuer 和 Realm 必须配置");
        }
        URI issuer;
        try {
            issuer = URI.create(trimTrailingSlash(issuerValue.trim()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Keycloak issuer URI 格式无效", exception);
        }
        if (!"https".equalsIgnoreCase(issuer.getScheme()) && !"http".equalsIgnoreCase(issuer.getScheme())) {
            throw new IllegalArgumentException("Keycloak issuer URI 必须使用 HTTP 或 HTTPS");
        }
        if (issuer.getHost() == null || issuer.getRawUserInfo() != null || issuer.getQuery() != null
                || issuer.getFragment() != null) {
            throw new IllegalArgumentException("Keycloak issuer URI 格式无效");
        }
        String expectedRealmPath = "/realms/" + realmValue.trim();
        if (!trimTrailingSlash(Optional.ofNullable(issuer.getPath()).orElse("")).endsWith(expectedRealmPath)) {
            throw new IllegalArgumentException("Keycloak issuer URI 应指向配置的 Realm");
        }
        return issuer;
    }

    private void requireSameOrigin(URI issuer, String endpoint, String metadataName) {
        URI endpointUri;
        try {
            endpointUri = URI.create(endpoint);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Keycloak Discovery 中的 " + metadataName + " 无效", exception);
        }
        if (endpointUri.getHost() == null || !issuer.getScheme().equalsIgnoreCase(endpointUri.getScheme())
                || !issuer.getHost().equalsIgnoreCase(endpointUri.getHost())
                || effectivePort(issuer) != effectivePort(endpointUri)) {
            throw new IllegalArgumentException("Keycloak Discovery 中的 " + metadataName + " 必须与 issuer 同源");
        }
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private void validateCallbackUri(String value, String fieldName) {
        try {
            URI uri = URI.create(value.trim());
            boolean validScheme = "https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme());
            if (!validScheme || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(fieldName + "格式无效");
        }
    }

    private URI adminRealmUri(String issuerValue, String realm) {
        URI issuer = URI.create(trimTrailingSlash(issuerValue.trim()));
        String realmSuffix = "/realms/" + realm;
        String path = Optional.ofNullable(issuer.getPath()).orElse("");
        String serverPath = path.substring(0, path.length() - realmSuffix.length());
        String url = issuer.getScheme() + "://" + issuer.getRawAuthority() + serverPath
                + "/admin/realms/" + URLEncoder.encode(realm, StandardCharsets.UTF_8);
        return URI.create(url);
    }

    private String requiredMetadata(JsonNode metadata, String key) {
        String value = metadata.path(key).asText();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Keycloak Discovery 未提供 " + key);
        }
        return value;
    }

    private boolean sameIssuer(String expected, String actual) {
        return trimTrailingSlash(expected).equals(trimTrailingSlash(actual));
    }

    private String trimTrailingSlash(String value) {
        String normalized = value;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

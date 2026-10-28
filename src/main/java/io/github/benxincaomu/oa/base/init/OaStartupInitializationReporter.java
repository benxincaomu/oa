package io.github.benxincaomu.oa.base.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.github.benxincaomu.oa.base.security.KeycloakConnectionService;
import io.github.benxincaomu.oa.base.security.KeycloakIntegrationConfig;
import io.github.benxincaomu.oa.bussiness.tenant.TenantRepository;

/**
 * Checks the file-based Keycloak configuration, the local tenant data and the
 * Keycloak links at startup, and writes the initialization URL to the log when the
 * application still needs initialization.
 *
 * <p>
 * The check reads the database directly (not the Redis "projectIsInitialized"
 * cache), so it stays correct after a database restore/reset while Redis still
 * holds an old value.
 * </p>
 */
@Component
public class OaStartupInitializationReporter implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(OaStartupInitializationReporter.class);

    private final KeycloakConnectionService connectionService;
    private final TenantRepository tenantRepository;
    private final InitService initService;

    @Value("${app.base-url:}")
    private String appBaseUrl;

    public OaStartupInitializationReporter(KeycloakConnectionService connectionService,
            TenantRepository tenantRepository, InitService initService) {
        this.connectionService = connectionService;
        this.tenantRepository = tenantRepository;
        this.initService = initService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            KeycloakIntegrationConfig config = connectionService.getConfiguration().orElse(null);
            long tenantCount = tenantRepository.count();
            boolean keycloakLinked = initService.hasKeycloakLinks();
            String initUrl = initializationUrl();

            if (config == null) {
                logKeycloakNotConfigured(initUrl);
            } else if (tenantCount == 0) {
                logInitializationRequired(initUrl, "尚未创建租户数据，Keycloak 配置已从配置文件读取");
            } else if (!keycloakLinked) {
                logInitializationRequired(initUrl,
                        "检测到已有租户数据，但管理员/根部门/管理员角色的 Keycloak 链接缺失（存量安装初始化）");
            } else {
                logger.info("OA 已就绪：Keycloak issuer={}, realm={}, tenants={}",
                        config.getIssuerUri(), config.getRealm(), tenantCount);
            }
        } catch (RuntimeException exception) {
            // 启动检查失败不应阻止应用启动，但错误必须在日志中可见
            logger.error("启动时检查 Keycloak/初始化状态失败", exception);
        }
    }

    private void logKeycloakNotConfigured(String initUrl) {
        logger.error("""
                =========================== KEYCLOAK NOT CONFIGURED ===========================
                未检测到完整的 Keycloak 配置。请在 application.yml（或对应环境变量）中配置：
                    app.keycloak.issuer-uri                    KEYCLOAK_ISSUER_URI
                    app.keycloak.realm                         KEYCLOAK_REALM
                    app.keycloak.client-id                     KEYCLOAK_CLIENT_ID
                    app.keycloak.client-secret                 KEYCLOAK_CLIENT_SECRET
                    app.keycloak.service-account-client-id     KEYCLOAK_SERVICE_ACCOUNT_CLIENT_ID
                    app.keycloak.service-account-client-secret KEYCLOAK_SERVICE_ACCOUNT_CLIENT_SECRET
                    app.keycloak.redirect-uri                  KEYCLOAK_REDIRECT_URI
                    app.keycloak.post-logout-redirect-uri      KEYCLOAK_POST_LOGOUT_REDIRECT_URI
                修改配置并重启应用后，打开初始化页面完成租户/管理员初始化：
                    {}
                注意：Keycloak 客户端密钥只从配置文件/环境变量读取，不会写入数据库或日志。
                =============================================================================
                """, initUrl);
    }

    private void logInitializationRequired(String initUrl, String reason) {
        logger.warn("""
                ========================= OA INITIALIZATION REQUIRED =========================
                {}
                请使用浏览器打开初始化页面：
                    {}
                说明：Keycloak 连接配置来自 application.yml / 环境变量（app.keycloak.*）；
                     初始化只创建本地租户/管理员快照并补齐 Keycloak 链接，不会修改已存在的工作流数据。
                =============================================================================
                """, reason, initUrl);
    }

    private String initializationUrl() {
        String baseUrl = StringUtils.hasText(appBaseUrl) ? appBaseUrl.trim() : "";
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (!StringUtils.hasText(baseUrl)) {
            return "/init（app.base-url 未配置，请使用前端实际地址 + /init）";
        }
        return baseUrl + "/init";
    }
}

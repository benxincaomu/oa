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
 * Checks the persisted Keycloak configuration and the local tenant at startup and
 * writes the initialization URL to the log when the application still needs to
 * be initialized.
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

    @Value("${app.base-url:}")
    private String appBaseUrl;

    public OaStartupInitializationReporter(KeycloakConnectionService connectionService,
            TenantRepository tenantRepository) {
        this.connectionService = connectionService;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            KeycloakIntegrationConfig config = connectionService.getConfiguration().orElse(null);
            long tenantCount = tenantRepository.count();
            String initUrl = initializationUrl();

            if (config == null) {
                String reason = tenantCount > 0
                        ? "未检测到 Keycloak 配置（数据库已有 " + tenantCount + " 个租户，将按存量安装初始化）"
                        : "未检测到 Keycloak 配置（数据库表 keycloak_integration_config 为空）";
                logInitializationRequired(initUrl, reason);
            } else if (tenantCount == 0) {
                logInitializationRequired(initUrl, "已存在 Keycloak 配置，但租户/管理员尚未初始化");
            } else {
                logger.info("OA 已初始化：Keycloak issuer={}, realm={}, tenants={}",
                        config.getIssuerUri(), config.getRealm(), tenantCount);
            }
        } catch (RuntimeException exception) {
            // 启动检查失败不应阻止应用启动，但错误必须在日志中可见
            logger.error("启动时检查 Keycloak/初始化状态失败", exception);
        }
    }

    private void logInitializationRequired(String initUrl, String reason) {
        logger.warn("""
                ========================= OA INITIALIZATION REQUIRED =========================
                {}
                请使用浏览器打开初始化页面：
                    {}
                初始化需要以下环境变量：
                    KEYCLOAK_SETUP_TOKEN              一次性 setup token（初始化请求校验用）
                    KEYCLOAK_CONFIG_ENCRYPTION_KEY    Base64 编码的 32 字节密钥（加密 Keycloak 客户端密钥）
                说明：Keycloak 配置保存在数据库表 keycloak_integration_config，初始化完成后该页面不再可用。
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

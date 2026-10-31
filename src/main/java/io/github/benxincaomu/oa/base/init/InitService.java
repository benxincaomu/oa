package io.github.benxincaomu.oa.base.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import io.github.benxincaomu.oa.base.security.KeycloakAdminClient;
import io.github.benxincaomu.oa.base.security.KeycloakConnectionService;

/**
 * Startup provisioning of the Keycloak authorization model.
 *
 * <p>
 * Users, groups and their memberships are managed in Keycloak and are no longer
 * created by OA. What OA still needs is its authorization model: the
 * {@code oa-admin} application client role, the baseline menu/API resources, the
 * role policy and the resource permissions. This runner checks those objects and
 * creates/updates the missing ones through the Keycloak Admin REST API. It is
 * idempotent and runs on every startup.
 * </p>
 *
 * <p>
 * The application only serves business requests once this provisioning succeeds
 * ({@link #isReady()}). A failure is logged and can be retried by fixing the
 * configuration/Keycloak and restarting.
 * </p>
 */
@Component
public class InitService implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(InitService.class);

    private final OaTenantProvider tenantProvider;
    private final KeycloakConnectionService connectionService;
    private final KeycloakAdminClient keycloakAdminClient;
    private final OaBaselineAuthorizationProvisioner baselineAuthorizationProvisioner;

    private volatile boolean ready;
    private volatile String failureReason;

    public InitService(OaTenantProvider tenantProvider,
            KeycloakConnectionService connectionService,
            KeycloakAdminClient keycloakAdminClient,
            OaBaselineAuthorizationProvisioner baselineAuthorizationProvisioner) {
        this.tenantProvider = tenantProvider;
        this.connectionService = connectionService;
        this.keycloakAdminClient = keycloakAdminClient;
        this.baselineAuthorizationProvisioner = baselineAuthorizationProvisioner;
    }

    @Override
    public void run(ApplicationArguments args) {
        initialize();
    }

    /**
     * Ensures the Keycloak role/resource/policy/permission objects required by OA
     * exist. Safe to re-run; never throws to the caller (startup must stay up so the
     * failure is visible in the log).
     */
    public synchronized void initialize() {
        if (ready) {
            return;
        }
        if (connectionService.getConfiguration().isEmpty()) {
            failureReason = "Keycloak 配置不完整";
            logger.error("""
                    =========================== KEYCLOAK NOT CONFIGURED ===========================
                    未检测到完整的 Keycloak 配置，OA 将保持未就绪状态。请在
                    src/main/filters/<profile>.properties（或对应环境变量）中配置：
                        keycloak.issuer-uri / keycloak.realm / keycloak.client-id / keycloak.client-secret
                        keycloak.service-account-client-id / keycloak.service-account-client-secret
                        keycloak.redirect-uri / keycloak.post-logout-redirect-uri
                    修改后重新编译并重启应用。
                    =============================================================================
                    """);
            return;
        }
        try {
            Long tenantId = tenantProvider.getTenantId();
            keycloakAdminClient.ensureAdminRole();
            baselineAuthorizationProvisioner.provision(tenantId);
            failureReason = null;
            ready = true;
            logger.info("Keycloak 角色与权限初始化完成：tenantId={}，已确保 oa-admin 角色、基础菜单/API 资源、"
                    + "角色策略和资源权限存在", tenantId);
        } catch (RuntimeException exception) {
            ready = false;
            failureReason = exception.getMessage();
            logger.error("Keycloak 角色/权限自动初始化失败，OA 保持未就绪：{}", exception.getMessage(), exception);
        }
    }

    public boolean isReady() {
        return ready;
    }

    /**
     * Kept for the legacy {@code SecurityFilter} which is not active in the Keycloak
     * profile.
     */
    public boolean isInitialized() {
        return ready;
    }

    public String getFailureReason() {
        return failureReason;
    }
}

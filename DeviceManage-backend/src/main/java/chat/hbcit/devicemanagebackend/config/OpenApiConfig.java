package chat.hbcit.devicemanagebackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 3 接口文档配置类
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI deviceManageOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("光影资产 (DeviceManage) API 接口文档")
                        .description("基于 Spring Boot 3、MyBatis-Plus、Spring Security + JWT、Redis 与定时任务构建的媒体设备全生命周期资产管理平台后端接口文档。")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("赵杰")
                                .email("kissfeng@power8.top"))
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请输入登录后获取的 JWT Token（无需手动加 Bearer 前缀）")));
    }

    @Bean
    public GroupedOpenApi userPortalApi() {
        return GroupedOpenApi.builder()
                .group("01-用户端与认证接口")
                .pathsToMatch("/auth/**", "/categories/**", "/devices/**", "/borrow/**", "/user/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminPortalApi() {
        return GroupedOpenApi.builder()
                .group("02-管理员端接口")
                .pathsToMatch("/admin/**")
                .build();
    }
}

package com.token.erp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI（Swagger UI）配置
 * - 自定义文档标题、版本、描述
 * - 配置 JWT Bearer 鉴权，Swagger UI 右上角 Authorize 可填 token 调试
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI tokenErpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Token 制造 ERP API")
                        .description("Token 制造 ERP 后端接口文档\n\n"
                                + "统一规范：\n"
                                + "- 返回结构 Result<T>（code=0 成功）\n"
                                + "- 分页返回 PageResult<T>（page/pageSize/total/records）\n"
                                + "- 编号规则 业务前缀+yyyyMMdd+4位流水\n"
                                + "- 鉴权：除登录外均需 Authorization: Bearer <token>")
                        .version("2.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("登录接口获取的 token，格式：Bearer <token>")));
    }
}

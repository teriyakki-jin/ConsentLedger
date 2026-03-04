package com.consentledger.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String JWT_SCHEME = "BearerAuth";
    private static final String API_KEY_SCHEME = "ApiKeyAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConsentLedger API")
                        .description("개인정보 전송요구권 관리 시스템 API")
                        .version("v1.0.0"))
                .addSecurityItem(new SecurityRequirement()
                        .addList(JWT_SCHEME)
                        .addList(API_KEY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(JWT_SCHEME, new SecurityScheme()
                                .name(JWT_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요 (사용자 인증)"))
                        .addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
                                .name(API_KEY_SCHEME)
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("Agent API 키를 입력하세요")));
    }
}

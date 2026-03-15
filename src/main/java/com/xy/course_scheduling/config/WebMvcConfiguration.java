package com.xy.course_scheduling.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    /**
     * 配置OpenAPI 3.0文档（替代原Swagger 2.0的Docket）
     */
    @Bean
    public OpenAPI openAPI() {
        log.info("开始生成接口文档...");
        return new OpenAPI()
                // 接口文档基本信息
                .info(new Info()
                        .title("排课项目接口文档")
                        .version("1.0")
                        .description("排课项目接口文档"));
    }

    /**
     * 静态资源映射（Knife4j需要）
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Knife4j文档页面
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        // 文档页面依赖的静态资源
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        // 补充OpenAPI 3.0的json资源映射（关键，否则文档数据加载失败）
        registry.addResourceHandler("/v3/api-docs/**")
                .addResourceLocations("classpath:/META-INF/resources/v3/api-docs/");
    }

}

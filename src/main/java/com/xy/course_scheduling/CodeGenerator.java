package com.xy.course_scheduling;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.util.Collections;

public class CodeGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://localhost:3309/course_scheduling?serverTimezone=UTC&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=convertToNull", "root", "123456")
                // 全局配置
                .globalConfig(builder -> {
                    builder.author("张三")         // 设置作者
                            .outputDir(System.getProperty("user.dir") + "/src/main/java") // 指定输出目录
                            .commentDate("yyyy-MM-dd") // 注释日期
                            .disableOpenDir();         // 生成后不打开文件夹
                })
                // 包配置
                .packageConfig(builder -> {
                    builder.parent("com.xy.course_scheduling")      // 父包名
                            //.moduleName("demo")        // 模块名（可选）
                            .entity("entity")
                            .mapper("mapper")
                            .service("service")
                            .serviceImpl("service.impl")
                            .controller("controller")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, System.getProperty("user.dir") + "/src/main/resources/mapper")); // 设置mapperXml生成路径
                    // 添加Vue文件输出路径
//                            .pathInfo(new HashMap<OutputFile, String>() {{
//                                put(OutputFile.xml, System.getProperty("user.dir") + "/src/main/resources/mapper");
//                                put(OutputFile.entity, System.getProperty("user.dir") + "/frontend/src/views");  // Vue页面输出路径
//                                put(OutputFile.service, System.getProperty("user.dir") + "/frontend/src/api");   // API文件输出路径
//                                put(OutputFile.other, System.getProperty("user.dir") + "/frontend/src/router");  // 路由文件输出路径
//                            }});
                })
                // 策略配置（核心修改：关闭字段驼峰转换）
                .strategyConfig(builder -> {
                    builder
                            .addInclude("tb_schedule_plan") // 生成的表名
                            .addTablePrefix("tb_") // 设置过滤表前缀
                            // 实体类配置
                            .entityBuilder()
                            //.columnNaming(NamingStrategy.no_change) // 保留原字段格式
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            // Controller 配置（生成增删改查接口）
                            .controllerBuilder()
                            .enableRestStyle() // 生成 @RestController
                            .enableHyphenStyle() // URL中使用连字符
                            .formatFileName("%sController") // 控controller.java.ftl
                            // Service 配置
                            .serviceBuilder()
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl")
                            .enableFileOverride();
                })
                // 关键配置：指定自定义模板的位置

                .templateConfig(builder -> {

                    builder.controller("/templates/controller.java.ftl"); // 指向您自定义的Controller模板文件
                    // 添加Vue模板配置
//                    builder.entity("/templates/vue-entity.vue.ftl");  // Vue页面组件模板
//                    builder.service("/templates/vue-api.js.ftl");     // API请求模板
//                    builder.mapper("/templates/vue-router.js.ftl");   // 路由配置模板
                })
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎
                .execute();
    }
}
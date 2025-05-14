# MXY Browser Use

MXY Browser Use 是一个Java版本的浏览器自动化框架，专为AI代理(Agent)设计，基于Spring AI和Spring AI Alibaba。它是Python项目 [browser-use](https://github.com/browser-use/browser-use) 的Java实现版本。

## 功能特点

* 基于Java 17和Spring Boot 3开发
* 使用Playwright实现可靠的浏览器自动化
* 与多种LLM模型集成(DashScope、OpenAI等)
* 支持DOM解析和元素交互
* 使用异步编程提高性能
* 支持浏览器状态管理和多标签页

## 项目结构

```
mxy-browser-use/
├── browser-use-core/ - 核心功能模块
├── browser-use-spring-boot-starter/ - Spring Boot Starter
└── browser-use-samples/ - 示例应用
```

## 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>com.mxy</groupId>
    <artifactId>browser-use-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 基本使用

```java
@Component
public class TaobaoShopping {

    private final Agent agent;
    private final DashScopeAiClient dashScopeClient;

    public TaobaoShopping(DashScopeAiClient dashScopeClient) {
        this.dashScopeClient = dashScopeClient;
        
        // 创建代理
        this.agent = Agent.builder()
                .task("1、打开淘宝，如果未登录等待登录， 2、给我找一款男鞋夏季穿的 3、点击立即购买")
                .llm(dashScopeClient.createChatModel())
                .enableMemory(false)
                .maxActionsPerStep(5)
                .build();
    }

    public void run() {
        try {
            // 运行自动化任务
            agent.run();
        } catch (Exception e) {
            log.error("运行失败", e);
        } finally {
            // 确保关闭浏览器
            agent.close();
        }
    }
}
```

## 技术栈

* Java 17
* Spring Boot 3.2.x
* Spring AI
* Spring AI Alibaba
* Playwright
* Jackson
* JSoup
* Lombok

## 许可证

Apache License 2.0 
package com.mxy.browser.use.samples;

import com.mxy.browser.use.agent.Agent;
import com.mxy.browser.use.browser.Browser;
import com.mxy.browser.use.browser.BrowserConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 淘宝购物示例应用
 */
@SpringBootApplication
@Slf4j
public class TaobaoShoppingSample {

    public static void main(String[] args) {
        SpringApplication.run(TaobaoShoppingSample.class, args);
    }


    /**
     * 示例运行器
     */
    @Component
    @Profile("!test")
    public static class SampleRunner implements CommandLineRunner {

        @Autowired
        private TaobaoShopper taobaoShopper;

        @Override
        public void run(String... args) throws Exception {
            log.info("启动淘宝购物自动化示例");
            
            // 启动购物任务
            CompletableFuture<Void> shoppingTask = taobaoShopper.shop();
            
            // 等待任务完成
            shoppingTask.join();
            
            log.info("购物任务已完成");
        }
    }

    /**
     * 淘宝购物执行器
     */
    @Component
    @Slf4j
    public static class TaobaoShopper {

        private final ChatClient chatClient;
        public TaobaoShopper(@Qualifier("dashscopeChatModel") ChatModel chatModel) {
            this.chatClient = ChatClient.builder(chatModel)
                    .defaultAdvisors(new SimpleLoggerAdvisor())
                    .defaultOptions(OpenAiChatOptions.builder().temperature(0.1).build())
                    .build();
        }
        
        /**
         * 执行购物任务
         */
        public CompletableFuture<Void> shop() {
            log.info("准备购物任务");
            
            // 配置浏览器
            BrowserConfig browserConfig = BrowserConfig.builder()
                    .headless(false) // 显示浏览器UI
                    .disableSecurity(false) // 保持安全设置
                    .build();
            
            // 创建浏览器实例
            Browser browser = new Browser(browserConfig);
            
            // 创建代理
            Agent agent = Agent.builder()
                    .task("1、打开淘宝，如果未登录等待登录， 2、给我找一款男鞋夏季穿的 3、点击立即购买 4、等待用户确认")
                    .llm(createChatClient())
                    .browser(browser)
                    .enableMemory(false)  // 禁用内存功能
                    .maxActionsPerStep(5) // 限制每步最大动作数
                    .build();
            
            return CompletableFuture.runAsync(() -> {
                try {
                    // 运行代理
                    agent.run();
                } catch (Exception e) {
                    log.error("购物任务失败", e);
                } finally {
                    // 关闭代理和浏览器
                    try {
                        agent.close();
                    } catch (Exception e) {
                        log.error("关闭代理失败", e);
                    }
                }
            });
        }
        
        /**
         * 创建ChatClient
         */
        private ChatClient createChatClient() {
           return chatClient;
        }

    }

} 
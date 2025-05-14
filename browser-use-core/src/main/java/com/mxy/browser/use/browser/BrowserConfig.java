package com.mxy.browser.use.browser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 浏览器配置
 * 对应Python版本的BrowserConfig类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowserConfig {

    /**
     * 是否使用无头模式 (不显示浏览器UI)
     * 注意：许多网站会检测并阻止无头浏览器
     */
    @Builder.Default
    private boolean headless = false;

    /**
     * 是否禁用浏览器安全功能
     * 警告：禁用安全功能可能导致安全风险，特别是访问不可信网站时
     */
    @Builder.Default
    private boolean disableSecurity = false;

    /**
     * 浏览器完成任务后是否保持活动状态
     */
    @Builder.Default
    private boolean keepAlive = false;

    /**
     * 浏览器类型: chromium, firefox, webkit
     */
    @Builder.Default
    private String browserClass = "chromium";

    /**
     * 浏览器远程调试端口
     */
    @Builder.Default
    private int chromeRemoteDebuggingPort = 9222;

    /**
     * 浏览器额外启动参数
     */
    @Builder.Default
    private List<String> extraBrowserArgs = new ArrayList<>();

    /**
     * 代理配置
     */
    private ProxySettings proxy;

    /**
     * 浏览器自定义二进制路径
     */
    private String browserBinaryPath;

    /**
     * 是否启用确定性渲染（用于测试）
     */
    @Builder.Default
    private boolean deterministicRendering = false;

    /**
     * CDP URL (用于连接到远程浏览器)
     */
    private String cdpUrl;

    /**
     * WSS URL (用于连接到远程浏览器)
     */
    private String wssUrl;

    /**
     * 浏览器上下文配置
     */
    @Builder.Default
    private BrowserContextConfig newContextConfig = new BrowserContextConfig();
} 
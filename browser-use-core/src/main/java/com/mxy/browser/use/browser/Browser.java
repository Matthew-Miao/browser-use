package com.mxy.browser.use.browser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Browser类 - 增强版Playwright浏览器
 * 
 * 这是一个持久的浏览器工厂，可以生成多个浏览器上下文。
 * 建议每个应用只使用一个Browser实例（否则内存消耗会增加）。
 */
@Slf4j
public class Browser {

    /**
     * 浏览器配置
     */
    @Getter
    private final BrowserConfig config;

    /**
     * Playwright实例
     */
    private Playwright playwright;

    /**
     * Playwright浏览器实例
     */
    private com.microsoft.playwright.Browser playwrightBrowser;

    /**
     * 创建一个新的Browser实例
     */
    public Browser() {
        this(null);
    }

    /**
     * 创建一个新的Browser实例
     * 
     * @param config 浏览器配置
     */
    public Browser(BrowserConfig config) {
        log.debug("🌎 初始化新浏览器");
        this.config = config != null ? config : new BrowserConfig();
    }

    /**
     * 创建新的浏览器上下文
     * 
     * @param config 上下文配置
     * @return 浏览器上下文
     */
    public CompletableFuture<BrowserContext> newContext(BrowserContextConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("创建新的浏览器上下文");
                BrowserConfig mergedConfig = config != null
                        ? mergeConfigs(this.config, config)
                        : this.config;
                
                return new BrowserContext(this, mergedConfig.getNewContextConfig());
            } catch (Exception e) {
                log.error("创建浏览器上下文失败", e);
                throw new RuntimeException("创建浏览器上下文失败", e);
            }
        });
    }

    /**
     * 获取Playwright浏览器实例
     * 
     * @return Playwright浏览器实例的CompletableFuture
     */
    public CompletableFuture<com.microsoft.playwright.Browser> getPlaywrightBrowser() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (playwrightBrowser == null) {
                    init().get(); // 等待初始化完成
                }
                return playwrightBrowser;
            } catch (InterruptedException | ExecutionException e) {
                log.error("获取Playwright浏览器实例失败", e);
                throw new RuntimeException("获取Playwright浏览器实例失败", e);
            }
        });
    }

    /**
     * 初始化浏览器会话
     * 
     * @return 初始化操作的CompletableFuture
     */
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("初始化浏览器会话");
                playwright = Playwright.create();
                setupBrowser();
            } catch (Exception e) {
                log.error("初始化浏览器会话失败", e);
                throw new RuntimeException("初始化浏览器会话失败", e);
            }
        });
    }

    /**
     * 设置浏览器
     */
    private void setupBrowser() {
        try {
            if (config.getCdpUrl() != null) {
                setupRemoteCdpBrowser();
            } else if (config.getWssUrl() != null) {
                setupRemoteWssBrowser();
            } else {
                if (config.isHeadless()) {
                    log.warn("⚠️ 不推荐使用无头模式。许多网站会检测并阻止所有无头浏览器。");
                }

                if (config.getBrowserBinaryPath() != null) {
                    setupUserProvidedBrowser();
                } else {
                    setupBuiltinBrowser();
                }
            }
        } catch (Exception e) {
            log.error("初始化Playwright浏览器失败: {}", e.getMessage());
            throw new RuntimeException("初始化Playwright浏览器失败", e);
        }
    }

    /**
     * 设置内置浏览器
     */
    private void setupBuiltinBrowser() {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(config.isHeadless())
                .setArgs(getChromiumArgs())
                .setIgnoreDefaultArgs(new ArrayList<>());

        // 设置代理
        if (config.getProxy() != null) {
            options.setProxy(new Proxy(config.getProxy().getServer())
                    .setBypass(config.getProxy().getBypass())
                    .setUsername(config.getProxy().getUsername())
                    .setPassword(config.getProxy().getPassword()));
        }

        // 根据浏览器类型选择不同的启动方式
        switch (config.getBrowserClass().toLowerCase()) {
            case "chromium":
                playwrightBrowser = playwright.chromium().launch(options);
                break;
            case "firefox":
                playwrightBrowser = playwright.firefox().launch(options);
                break;
            case "webkit":
                playwrightBrowser = playwright.webkit().launch(options);
                break;
            default:
                throw new IllegalArgumentException("不支持的浏览器类型: " + config.getBrowserClass());
        }
    }

    /**
     * 设置用户提供的浏览器
     */
    private void setupUserProvidedBrowser() {
        // 暂未实现
        throw new UnsupportedOperationException("暂未实现用户提供的浏览器功能");
    }

    /**
     * 设置远程CDP浏览器
     */
    private void setupRemoteCdpBrowser() {
        // 暂未实现
        throw new UnsupportedOperationException("暂未实现远程CDP浏览器功能");
    }

    /**
     * 设置远程WSS浏览器
     */
    private void setupRemoteWssBrowser() {
        // 暂未实现
        throw new UnsupportedOperationException("暂未实现远程WSS浏览器功能");
    }

    /**
     * 获取Chromium浏览器参数
     * 
     * @return Chromium参数列表
     */
    private List<String> getChromiumArgs() {
        List<String> args = new ArrayList<>();

        // 添加常用的Chrome参数
        args.add("--no-sandbox");
        args.add("--disable-setuid-sandbox");
        args.add("--disable-dev-shm-usage");
        args.add("--disable-accelerated-2d-canvas");
        args.add("--no-first-run");
        args.add("--no-zygote");
        args.add("--disable-gpu");
        args.add("--hide-scrollbars");
        args.add("--mute-audio");
        
        // 如果在Docker中，添加Docker特定的参数
        if (isRunningInDocker()) {
            args.add("--disable-dev-shm-usage");
            args.add("--disable-features=site-per-process");
            args.add("--disable-extensions");
        }
        
        // 如果需要无头模式，添加相关参数
        if (config.isHeadless()) {
            args.add("--headless=new");
        }
        
        // 如果禁用安全性，添加相关参数
        if (config.isDisableSecurity()) {
            args.add("--disable-web-security");
            args.add("--allow-running-insecure-content");
            args.add("--disable-features=IsolateOrigins,site-per-process");
        }
        
        // 添加用户指定的额外参数
        if (config.getExtraBrowserArgs() != null) {
            args.addAll(config.getExtraBrowserArgs());
        }
        
        return args;
    }

    /**
     * 检查是否在Docker环境中运行
     * 
     * @return 是否在Docker中运行
     */
    private boolean isRunningInDocker() {
        try {
            // 检查cgroup信息是否包含docker相关内容
            java.nio.file.Path cgroupPath = java.nio.file.Paths.get("/proc/self/cgroup");
            if (java.nio.file.Files.exists(cgroupPath)) {
                String content = new String(java.nio.file.Files.readAllBytes(cgroupPath));
                return content.contains("docker") || content.contains("kubepods");
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 合并浏览器配置
     * 
     * @param browserConfig 浏览器配置
     * @param contextConfig 上下文配置
     * @return 合并后的浏览器配置
     */
    private BrowserConfig mergeConfigs(BrowserConfig browserConfig, BrowserContextConfig contextConfig) {
        // 创建一个新的浏览器配置，复制原有的配置
        BrowserConfig mergedConfig = BrowserConfig.builder()
                .headless(browserConfig.isHeadless())
                .disableSecurity(browserConfig.isDisableSecurity())
                .keepAlive(browserConfig.isKeepAlive())
                .browserClass(browserConfig.getBrowserClass())
                .chromeRemoteDebuggingPort(browserConfig.getChromeRemoteDebuggingPort())
                .extraBrowserArgs(new ArrayList<>(browserConfig.getExtraBrowserArgs()))
                .proxy(browserConfig.getProxy())
                .browserBinaryPath(browserConfig.getBrowserBinaryPath())
                .deterministicRendering(browserConfig.isDeterministicRendering())
                .cdpUrl(browserConfig.getCdpUrl())
                .wssUrl(browserConfig.getWssUrl())
                .newContextConfig(contextConfig)
                .build();
        
        return mergedConfig;
    }

    /**
     * 关闭浏览器
     * 
     * @return 关闭操作的CompletableFuture
     */
    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (playwrightBrowser != null) {
                    playwrightBrowser.close();
                    playwrightBrowser = null;
                }
                if (playwright != null) {
                    playwright.close();
                    playwright = null;
                }
                log.debug("浏览器已关闭");
            } catch (Exception e) {
                log.error("关闭浏览器失败", e);
            }
        });
    }
} 
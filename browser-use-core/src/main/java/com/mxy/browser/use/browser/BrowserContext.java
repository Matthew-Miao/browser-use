package com.mxy.browser.use.browser;

import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.BrowserContext.*;
import com.microsoft.playwright.BrowserContext.StorageStateOptions;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.Geolocation;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.ServiceWorkerPolicy;
import com.microsoft.playwright.options.ViewportSize;
import com.mxy.browser.use.dom.DomElement;
import com.mxy.browser.use.dom.DomService;
import com.mxy.browser.use.dom.DomState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 浏览器上下文
 * 对Playwright BrowserContext的增强封装
 */
@Slf4j
public class BrowserContext implements AutoCloseable {

    /**
     * 浏览器导航栏高度（像素）
     */
    private static final int BROWSER_NAVBAR_HEIGHT = 85;

    /**
     * 关联的浏览器实例
     */
    private final Browser browser;

    /**
     * 浏览器上下文配置
     */
    @Getter
    private final BrowserContextConfig config;

    /**
     * Playwright浏览器上下文
     */
    private com.microsoft.playwright.BrowserContext playwrightContext;

    /**
     * 当前页面状态
     */
    private BrowserState cachedState;

    /**
     * 用于跟踪最后一个活动标签页
     */
    private int lastActivePageId = 0;
    
    /**
     * 标签页列表
     */
    private final List<TabInfo> tabs = new ArrayList<>();
    
    /**
     * 所有网络请求的计数
     */
    private final AtomicInteger pendingNetworkRequests = new AtomicInteger(0);
    
    /**
     * 网络处理器是否已设置标记
     */
    private final AtomicBoolean networkHandlersRegistered = new AtomicBoolean(false);
    
    /**
     * 上一个可点击元素哈希缓存
     */
    private CachedStateClickableElementsHashes lastClickableElementsHashes;
    
    /**
     * 正在下载的文件集合
     */
    private final Set<String> downloadingFiles = ConcurrentHashMap.newKeySet();
    
    /**
     * 当前上下文的唯一标识符
     */
    private final String contextId = UUID.randomUUID().toString();

    public void updateCachedState(BrowserState newState) {
        this.cachedState = newState;
    }

    /**
     * 缓存的可点击元素哈希数据类
     */
    private static class CachedStateClickableElementsHashes {
        private final String url;
        private final Set<String> hashes;

        public CachedStateClickableElementsHashes(String url, Set<String> hashes) {
            this.url = url;
            this.hashes = hashes;
        }
    }

    /**
     * 创建浏览器上下文
     * 
     * @param browser 关联的浏览器实例
     * @param config 上下文配置
     */
    public BrowserContext(Browser browser, BrowserContextConfig config) {
        this.browser = browser;
        this.config = config != null ? config : new BrowserContextConfig();
    }
    
    /**
     * 获取浏览器会话
     * 
     * @return 浏览器会话
     */
    public CompletableFuture<BrowserSession> getSession() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (playwrightContext == null) {
                    initializeSession().get();
                }
                return new BrowserSession(playwrightContext, cachedState);
            } catch (InterruptedException | ExecutionException e) {
                log.error("获取浏览器会话失败", e);
                throw new RuntimeException("获取浏览器会话失败", e);
            }
        });
    }
    
    /**
     * 初始化浏览器会话
     * 
     * @return 初始化操作的CompletableFuture
     */
    public CompletableFuture<Void> initializeSession() {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("初始化浏览器会话");
                
                // 如果已有会话，先关闭
                if (playwrightContext != null) {
                    playwrightContext.close();
                    playwrightContext = null;
                }
                
                // 获取Playwright浏览器实例
                com.microsoft.playwright.Browser playwrightBrowser = browser.getPlaywrightBrowser().get();
                
                // 创建浏览器上下文
                playwrightContext = createContext(playwrightBrowser);
                
                // 创建新页面
                if (playwrightContext.pages().isEmpty()) {
                    playwrightContext.newPage();
                }
                
                // 初始化标签页列表
                updateTabsInfo();
                
                // 添加标签页可见性变化监听
                addTabForegroundingListener(playwrightContext.pages().get(0));
                
                // 设置网络空闲监听
                setupNetworkIdleListener();
                
            } catch (Exception e) {
                log.error("初始化浏览器会话失败", e);
                throw new RuntimeException("初始化浏览器会话失败", e);
            }
        });
    }

    /**
     * 创建Playwright浏览器上下文
     *
     * @param playwrightBrowser Playwright浏览器实例
     * @return Playwright浏览器上下文
     */
    private com.microsoft.playwright.BrowserContext createContext(com.microsoft.playwright.Browser playwrightBrowser) {
        // 准备上下文选项
        NewContextOptions options = new NewContextOptions()
                .setIgnoreHTTPSErrors(true)
                .setJavaScriptEnabled(true);

        // 设置视口大小
        if (!config.isNoViewport()) {
            options.setViewportSize(config.getWindowWidth(), config.getWindowHeight());
        }

        // 设置用户代理
        if (config.getUserAgent() != null) {
            options.setUserAgent(config.getUserAgent());
        }

        // 设置地理位置
        if (config.getGeolocation() != null) {
            Map<String, Object> geolocation = config.getGeolocation();
            Double latitude = (Double) geolocation.get("latitude");
            Double longitude = (Double) geolocation.get("longitude");
            Double accuracy = (Double) geolocation.getOrDefault("accuracy", 1.0);
            if (latitude != null && longitude != null) {
                Geolocation geo = new Geolocation(latitude, longitude);
                geo.setAccuracy(accuracy);
                options.setGeolocation(geo);
            }
        }

        // 设置移动设备模拟
        if (config.getIsMobile() != null) {
            options.setIsMobile(config.getIsMobile());
        }

        // 设置触摸事件模拟
        if (config.getHasTouch() != null) {
            options.setHasTouch(config.getHasTouch());
        }

        // 设置时区
        if (config.getTimezoneId() != null) {
            options.setTimezoneId(config.getTimezoneId());
        }

        // 设置区域
        if (config.getLocale() != null) {
            options.setLocale(config.getLocale());
        }

        // 设置HTTP凭据
        if (config.getHttpCredentials() != null) {
            Map<String, String> credentials = config.getHttpCredentials();
            options.setHttpCredentials(
                    credentials.get("username"),
                    credentials.get("password"));
        }

        // 设置Cookie文件
        if (config.getCookiesFile() != null) {
            try {
                // 从文件加载Cookie
                Path cookiesPath = Paths.get(config.getCookiesFile());
                if (java.nio.file.Files.exists(cookiesPath)) {
                    options.setStorageStatePath(cookiesPath);
                }
            } catch (Exception e) {
                log.warn("加载Cookie文件失败: {}", e.getMessage());
            }
        }

        // 设置下载路径
        if (config.getSaveDownloadsPath() != null) {
            options.setAcceptDownloads(true);

            try {
                Path downloadsPath = Paths.get(config.getSaveDownloadsPath());
                if (!java.nio.file.Files.exists(downloadsPath)) {
                    java.nio.file.Files.createDirectories(downloadsPath);
                }
                // 正确设置下载路径
                options.setRecordVideoDir(downloadsPath);
            } catch (Exception e) {
                log.warn("设置下载路径失败: {}", e.getMessage());
            }
        }

        // 设置录制路径
        if (config.getSaveRecordingPath() != null) {
            try {
                Path recordingPath = Paths.get(config.getSaveRecordingPath());
                if (!java.nio.file.Files.exists(recordingPath)) {
                    java.nio.file.Files.createDirectories(recordingPath);
                }
                options.setRecordVideoDir(recordingPath);
                options.setRecordVideoSize(config.getWindowWidth(), config.getWindowHeight());
            } catch (Exception e) {
                log.warn("设置录制路径失败: {}", e.getMessage());
            }
        }

        // 设置HAR文件路径
        if (config.getSaveHarPath() != null) {
            try {
                Path harPath = Paths.get(config.getSaveHarPath());
                if (!java.nio.file.Files.exists(harPath.getParent())) {
                    java.nio.file.Files.createDirectories(harPath.getParent());
                }
                options.setRecordHarPath(harPath);
                options.setRecordHarOmitContent(false);
            } catch (Exception e) {
                log.warn("设置HAR路径失败: {}", e.getMessage());
            }
        }

        // 创建浏览器上下文
        com.microsoft.playwright.BrowserContext context = playwrightBrowser.newContext(options);

        // 设置浏览器权限
        if (config.getPermissions() != null && !config.getPermissions().isEmpty()) {
            // 确保权限列表不为null且不为空
            List<String> permissions = new ArrayList<>(config.getPermissions());
            if (!permissions.isEmpty()) {
                // 检查是否有有效的权限
                log.debug("设置浏览器权限: {}", permissions);
                context.grantPermissions(permissions);
            }
        }

        // 设置Service Worker策略 - 使用枚举常量
//        context.set(ServiceWorkerPolicy.ALLOW);
//
//        // 添加下载监听器
//        context.onDownload(download -> {
//            String suggestedFilename = download.suggestedFilename();
//            log.info("正在下载文件: {}", suggestedFilename);
//
//            downloadingFiles.add(suggestedFilename);
//            String downloadPath = null;
//
//            if (config.getSaveDownloadsPath() != null) {
//                try {
//                    Path path = download.path();
//                    if (path != null) {
//                        downloadPath = path.toString();
//                        log.info("文件下载到: {}", downloadPath);
//                    }
//                } catch (Exception e) {
//                    log.warn("获取下载路径失败: {}", e.getMessage());
//                }
//            }
//
//            downloadingFiles.remove(suggestedFilename);
//        });

        return context;
    }
    
    /**
     * 设置网络空闲监听
     */
    private void setupNetworkIdleListener() {
        if (networkHandlersRegistered.compareAndSet(false, true)) {
            log.debug("设置网络监听");
            
            // 获取当前页面
            Page page = getCurrentPage();
            
            // 添加请求监听器
            page.onRequest(request -> {
                pendingNetworkRequests.incrementAndGet();
            });
            
            // 添加响应监听器
            page.onResponse(response -> {
                pendingNetworkRequests.decrementAndGet();
            });
            
            // 添加请求失败监听器
            page.onRequestFailed(request -> {
                pendingNetworkRequests.decrementAndGet();
            });
            
            // 添加请求完成监听器
            page.onRequestFinished(request -> {
                pendingNetworkRequests.decrementAndGet();
            });
        }
    }
    
    /**
     * 添加标签页可见性变化监听
     * 
     * @param page 页面对象
     */
    private void addTabForegroundingListener(Page page) {
        page.onPageError(error -> {
            log.warn("页面JavaScript错误: {}", error);
        });
        
        page.onClose(p -> {
            log.debug("页面已关闭: {}", page.url());
            updateTabsInfo();
        });
    }
    
    /**
     * 更新标签页信息
     */
    private void updateTabsInfo() {
        tabs.clear();
        
        List<Page> pages = playwrightContext.pages();
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            boolean isActive = (i == lastActivePageId);
            
            tabs.add(TabInfo.builder()
                    .pageId(i)
                    .url(page.url())
                    .title(page.title())
                    .isActive(isActive)
                    .build());
        }
    }
    
    /**
     * 获取当前页面
     * 
     * @return 当前页面对象
     */
    public Page getCurrentPage() {
        List<Page> pages = playwrightContext.pages();
        
        if (pages.isEmpty()) {
            throw new RuntimeException("浏览器没有打开的页面");
        }
        
        if (lastActivePageId >= pages.size()) {
            lastActivePageId = pages.size() - 1;
        }
        
        return pages.get(lastActivePageId);
    }
    
    /**
     * 关闭上下文
     */
    @Override
    public void close() {
        try {
            if (playwrightContext != null) {
                if (config.getCookiesFile() != null) {
                    saveCookies().get();
                }
                
                if (!config.isKeepAlive()) {
                    playwrightContext.close();
                    playwrightContext = null;
                    log.debug("浏览器上下文已关闭");
                } else {
                    log.debug("保持浏览器上下文活动状态");
                }
            }
        } catch (Exception e) {
            log.error("关闭浏览器上下文失败", e);
        }
    }
    
    /**
     * 保存Cookie
     * 
     * @return 保存操作的CompletableFuture
     */
    public CompletableFuture<Void> saveCookies() {
        return CompletableFuture.runAsync(() -> {
            if (config.getCookiesFile() == null || playwrightContext == null) {
                return;
            }
            
            try {
                log.debug("保存Cookie到: {}", config.getCookiesFile());
                String storageState = playwrightContext.storageState(
                        new StorageStateOptions().setPath(Paths.get(config.getCookiesFile())));
                log.debug("Cookie已保存");
            } catch (Exception e) {
                log.error("保存Cookie失败: {}", e.getMessage());
            }
        });
    }

}
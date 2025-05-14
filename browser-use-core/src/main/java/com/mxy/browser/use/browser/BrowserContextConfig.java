package com.mxy.browser.use.browser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 浏览器上下文配置
 * 对应Python版本的BrowserContextConfig类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowserContextConfig {

    /**
     * Cookie文件路径，用于持久化会话
     */
    private String cookiesFile;

    /**
     * 页面加载的最小等待时间（秒）
     */
    @Builder.Default
    private double minimumWaitPageLoadTime = 0.25;

    /**
     * 等待网络空闲的时间（秒）
     */
    @Builder.Default
    private double waitForNetworkIdlePageLoadTime = 0.5;

    /**
     * 页面加载的最大等待时间（秒）
     */
    @Builder.Default
    private double maximumWaitPageLoadTime = 5.0;

    /**
     * 多个操作之间的等待时间（秒）
     */
    @Builder.Default
    private double waitBetweenActions = 0.5;

    /**
     * 是否禁用浏览器安全功能
     * 警告：禁用安全功能可能导致安全风险，特别是访问不可信网站时
     */
    @Builder.Default
    private boolean disableSecurity = false;

    /**
     * 窗口宽度（像素）
     */
    @Builder.Default
    private int windowWidth = 1280;

    /**
     * 窗口高度（像素）
     */
    @Builder.Default
    private int windowHeight = 1100;

    /**
     * 是否使用真实窗口大小而不是固定视口
     * true：浏览器窗口大小决定视口大小（默认）
     * false：强制使用固定视口大小
     */
    @Builder.Default
    private boolean noViewport = true;

    /**
     * 视频录制保存路径
     */
    private String saveRecordingPath;

    /**
     * 下载文件保存路径
     */
    private String saveDownloadsPath;

    /**
     * HAR文件保存路径
     */
    private String saveHarPath;

    /**
     * 追踪文件保存路径
     */
    private String tracePath;

    /**
     * 区域设置，例如：en-GB, de-DE
     */
    private String locale;

    /**
     * 自定义用户代理
     */
    private String userAgent;

    /**
     * 是否高亮显示元素
     */
    @Builder.Default
    private boolean highlightElements = true;

    /**
     * 视口扩展（像素）
     * 控制LLM能看到的元素范围
     * -1：包含所有元素，无论是否可见（高Token消耗）
     * 0：仅包含视口内可见元素
     * 500（默认）：包含视口内元素和周围额外500像素区域内的元素
     */
    @Builder.Default
    private int viewportExpansion = 0;

    /**
     * 允许访问的域名列表
     * 如果为null，允许访问所有域名
     * 例如：["google.com", "wikipedia.org"]
     */
    private List<String> allowedDomains;

    /**
     * 是否在CSS选择器中包含动态属性
     */
    @Builder.Default
    private boolean includeDynamicAttributes = true;

    /**
     * HTTP认证凭据
     * 用于企业内网的HTTP基本认证
     * 例如：{"username": "user", "password": "pass"}
     */
    private Map<String, String> httpCredentials;

    /**
     * 浏览器任务完成后是否保持上下文（会话）活动
     */
    @Builder.Default
    private boolean keepAlive = false;

    /**
     * 是否模拟移动设备
     */
    private Boolean isMobile;

    /**
     * 是否启用触摸事件
     */
    private Boolean hasTouch;

    /**
     * 地理位置
     * 例如：{"latitude": 59.95, "longitude": 30.31667}
     */
    private Map<String, Object> geolocation;

    /**
     * 授予的浏览器权限
     * 例如：["clipboard-read", "clipboard-write", "geolocation"]
     */
    @Builder.Default
    private List<String> permissions = new ArrayList<String>() {{
        add("clipboard-read");
        add("clipboard-write");
    }};

    /**
     * 时区ID
     * 例如："Europe/Berlin"
     */
    private String timezoneId;

    /**
     * 是否强制创建新上下文
     * 在使用自定义配置和本地品牌浏览器（如Chrome、Edge）时很有用
     */
    @Builder.Default
    private boolean forceNewContext = false;

} 
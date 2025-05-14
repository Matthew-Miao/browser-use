package com.mxy.browser.use.browser;

import com.mxy.browser.use.dom.DomElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 浏览器状态
 * 保存当前浏览器页面的状态，供LLM分析和操作
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowserState {

    /**
     * 当前页面的URL
     */
    private String url;

    /**
     * 当前页面的标题
     */
    private String title;

    /**
     * DOM树
     */
    private DomElement elementTree;

    /**
     * 元素选择器映射表
     * 索引 -> DOM元素
     */
    private Map<Integer, DomElement> selectorMap;

    /**
     * 页面截图（Base64编码）
     */
    private String screenshot;

    /**
     * 当前页面可点击元素的信息
     */
    private List<ElementInfo> clickableElements;

    /**
     * 浏览器打开的标签页信息
     */
    private List<TabInfo> tabs;

    /**
     * 获取当前页面的简短信息
     * 
     * @return 包含URL和标题的字符串
     */
    public String getPageInfo() {
        return String.format("当前页面: %s (%s)", title, url);
    }

    /**
     * 检查URL是否有效
     * 
     * @return 是否有效
     */
    public boolean hasValidUrl() {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }
} 
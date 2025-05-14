package com.mxy.browser.use.browser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 浏览器标签页信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TabInfo {
    
    /**
     * 标签页ID
     */
    private int pageId;
    
    /**
     * 标签页URL
     */
    private String url;
    
    /**
     * 标签页标题
     */
    private String title;
    
    /**
     * 是否为当前活动标签页
     */
    private boolean isActive;
    
    /**
     * 获取标签页简短描述
     * 
     * @return 标签页描述
     */
    public String getDescription() {
        String activeMarker = isActive ? "✓ " : "";
        return String.format("%s[%d] %s (%s)", activeMarker, pageId, title, url);
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
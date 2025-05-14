package com.mxy.browser.use.dom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 浏览器视口信息
 * 包含视口尺寸
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewportInfo {
    
    /**
     * 视口宽度（像素）
     */
    private int width;
    
    /**
     * 视口高度（像素）
     */
    private int height;
    
    /**
     * 获取视口尺寸
     * 
     * @return 视口尺寸字符串表示
     */
    public String getDimensions() {
        return String.format("%dx%d", width, height);
    }
} 
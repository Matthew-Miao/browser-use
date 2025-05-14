package com.mxy.browser.use.browser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 页面元素信息
 * 存储页面中可交互元素的信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElementInfo {
    
    /**
     * 元素索引，用于在交互时引用
     */
    private int index;
    
    /**
     * 元素类型 (例如: "button", "input", "a")
     */
    private String tagName;
    
    /**
     * 元素内容文本
     */
    private String text;
    
    /**
     * 元素属性
     */
    private Map<String, String> attributes;
    
    /**
     * 元素对应的XPath
     */
    private String xpath;
    
    /**
     * 元素是否在视口内可见
     */
    private boolean isInViewport;
    
    /**
     * 元素是否可交互
     */
    private boolean isInteractive;
    
    /**
     * 元素的CSS选择器
     */
    private String cssSelector;
    
    /**
     * 获取元素ID (如果有)
     * 
     * @return 元素ID或null
     */
    public String getId() {
        return attributes.get("id");
    }
    
    /**
     * 获取元素名称 (如果有)
     * 
     * @return 元素名称或null
     */
    public String getName() {
        return attributes.get("name");
    }
    
    /**
     * 获取元素类型 (如果有)
     * 
     * @return 元素类型或null
     */
    public String getType() {
        return attributes.get("type");
    }
    
    /**
     * 获取元素值 (如果有)
     * 
     * @return 元素值或null
     */
    public String getValue() {
        return attributes.get("value");
    }
    
    /**
     * 获取元素ARIA标签 (如果有)
     * 
     * @return 元素ARIA标签或null
     */
    public String getAriaLabel() {
        return attributes.get("aria-label");
    }
    
    /**
     * 获取元素占位符 (如果有)
     * 
     * @return 元素占位符或null
     */
    public String getPlaceholder() {
        return attributes.get("placeholder");
    }
    
    /**
     * 获取元素简短描述
     * 
     * @return 元素描述
     */
    public String getShortDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%d] %s", index, tagName));
        
        if (text != null && !text.isBlank()) {
            if (text.length() > 50) {
                sb.append(String.format(" \"%s...\"", text.substring(0, 47)));
            } else {
                sb.append(String.format(" \"%s\"", text));
            }
        }
        
        String type = getType();
        if (type != null) {
            sb.append(String.format(" (type=%s)", type));
        }
        
        String id = getId();
        if (id != null) {
            sb.append(String.format(" (id=%s)", id));
        }
        
        return sb.toString();
    }
} 
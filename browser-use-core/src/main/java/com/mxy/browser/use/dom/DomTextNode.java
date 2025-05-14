package com.mxy.browser.use.dom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DOM文本节点
 * 表示页面中的文本内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomTextNode implements DomNode {
    
    /**
     * 文本内容
     */
    private String text;
    
    /**
     * 是否可见
     */
    private boolean isVisible;
    
    /**
     * 父节点
     */
    private DomElement parent;
    
    /**
     * 获取文本内容的简短描述
     * 
     * @return 文本内容
     */
    public String getShortDescription() {
        if (text == null || text.isEmpty()) {
            return "(empty text)";
        }
        
        if (text.length() <= 50) {
            return text;
        }
        
        return text.substring(0, 47) + "...";
    }
} 
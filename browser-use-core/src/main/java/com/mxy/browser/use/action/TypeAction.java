package com.mxy.browser.use.action;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 输入动作
 * 用于在页面元素中输入文本
 */
@Data
@AllArgsConstructor
public class TypeAction implements Action {
    
    /**
     * 元素索引
     */
    private int index;
    
    /**
     * 要输入的文本
     */
    private String text;
    
    @Override
    public String getType() {
        return "type";
    }
    
    @Override
    public String getDescription() {
        return "在索引为 " + index + " 的元素中输入: " + text;
    }
} 
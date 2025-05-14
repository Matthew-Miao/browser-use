package com.mxy.browser.use.action;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 点击动作
 * 用于点击页面中的元素
 */
@Data
@AllArgsConstructor
public class ClickAction implements Action {
    
    /**
     * 元素索引
     */
    private int index;
    
    @Override
    public String getType() {
        return "click";
    }
    
    @Override
    public String getDescription() {
        return "点击索引为 " + index + " 的元素";
    }
} 
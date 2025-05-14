package com.mxy.browser.use.action;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 导航动作
 * 用于导航到指定URL
 */
@Data
@AllArgsConstructor
public class NavigateAction implements Action {
    
    /**
     * 目标URL
     */
    private String url;
    
    @Override
    public String getType() {
        return "navigate";
    }
    
    @Override
    public String getDescription() {
        return "导航到URL: " + url;
    }
} 
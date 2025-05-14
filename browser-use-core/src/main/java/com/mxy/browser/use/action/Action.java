package com.mxy.browser.use.action;

/**
 * 动作接口
 * 定义浏览器自动化动作的基本方法
 */
public interface Action {
    
    /**
     * 获取动作类型
     * 
     * @return 动作类型
     */
    String getType();
    
    /**
     * 获取动作描述
     * 
     * @return 动作描述
     */
    String getDescription();
} 
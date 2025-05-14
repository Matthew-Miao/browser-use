package com.mxy.browser.use.memory;

import java.util.List;

/**
 * 记忆接口
 * 定义Agent记忆功能
 */
public interface Memory {
    
    /**
     * 添加记忆
     * 
     * @param key 记忆键
     * @param value 记忆值
     */
    void add(String key, String value);
    
    /**
     * 获取记忆
     * 
     * @param key 记忆键
     * @return 记忆值
     */
    String get(String key);
    
    /**
     * 检查是否存在记忆
     * 
     * @param key 记忆键
     * @return 是否存在
     */
    boolean has(String key);
    
    /**
     * 获取所有记忆键
     * 
     * @return 记忆键列表
     */
    List<String> keys();
    
    /**
     * 清除特定记忆
     * 
     * @param key 记忆键
     */
    void clear(String key);
    
    /**
     * 清除所有记忆
     */
    void clearAll();
} 
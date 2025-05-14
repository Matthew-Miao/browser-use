package com.mxy.browser.use.memory;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单内存实现
 * 使用Map存储键值对记忆
 */
@Slf4j
public class SimpleMemory implements Memory {
    
    /**
     * 记忆存储
     */
    private final Map<String, String> memoryMap = new ConcurrentHashMap<>();
    
    @Override
    public void add(String key, String value) {
        if (key == null || key.isEmpty()) {
            log.warn("无法添加记忆：键为空");
            return;
        }
        
        memoryMap.put(key, value);
        log.debug("添加记忆: {} -> {}", key, value);
    }
    
    @Override
    public String get(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        
        return memoryMap.get(key);
    }
    
    @Override
    public boolean has(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        
        return memoryMap.containsKey(key);
    }
    
    @Override
    public List<String> keys() {
        return new ArrayList<>(memoryMap.keySet());
    }
    
    @Override
    public void clear(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        
        memoryMap.remove(key);
        log.debug("清除记忆: {}", key);
    }
    
    @Override
    public void clearAll() {
        memoryMap.clear();
        log.debug("清除所有记忆");
    }
    
    /**
     * 获取所有记忆的副本
     * 
     * @return 记忆映射的副本
     */
    public Map<String, String> getAll() {
        return new HashMap<>(memoryMap);
    }
} 
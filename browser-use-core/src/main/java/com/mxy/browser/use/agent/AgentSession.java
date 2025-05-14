package com.mxy.browser.use.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代理会话
 * 保存Agent执行过程中的会话状态
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentSession {
    
    /**
     * 当前步骤
     */
    @Builder.Default
    private int currentStep = 0;
    
    /**
     * 会话状态
     */
    @Builder.Default
    private Map<String, Object> state = new HashMap<>();
    
    /**
     * 历史动作和结果
     */
    @Builder.Default
    private List<ActionRecord> history = new ArrayList<>();
    
    /**
     * 设置状态值
     * 
     * @param key 键
     * @param value 值
     */
    public void setState(String key, Object value) {
        state.put(key, value);
    }
    
    /**
     * 获取状态值
     * 
     * @param key 键
     * @param <T> 值类型
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public <T> T getState(String key) {
        return (T) state.get(key);
    }
    
    /**
     * 添加历史记录
     * 
     * @param actionRecord 动作记录
     */
    public void addHistory(ActionRecord actionRecord) {
        history.add(actionRecord);
    }
    
    /**
     * 获取最近的历史记录
     * 
     * @param count 数量
     * @return 历史记录列表
     */
    public List<ActionRecord> getRecentHistory(int count) {
        if (history.isEmpty() || count <= 0) {
            return new ArrayList<>();
        }
        
        int start = Math.max(0, history.size() - count);
        return new ArrayList<>(history.subList(start, history.size()));
    }
    
    /**
     * 动作记录
     * 记录执行的动作和结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionRecord {
        
        /**
         * 步骤
         */
        private int step;
        
        /**
         * 动作类型
         */
        private String actionType;
        
        /**
         * 动作描述
         */
        private String description;
        
        /**
         * 是否成功
         */
        private boolean success;
        
        /**
         * 结果消息
         */
        private String message;
    }
} 
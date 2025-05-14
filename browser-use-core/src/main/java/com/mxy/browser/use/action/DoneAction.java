package com.mxy.browser.use.action;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 完成动作
 * 用于标记任务完成
 */
@Data
@AllArgsConstructor
public class DoneAction implements Action {
    
    /**
     * 是否成功完成
     */
    private boolean success;
    
    /**
     * 结果消息
     */
    private String message;
    
    @Override
    public String getType() {
        return "done";
    }
    
    @Override
    public String getDescription() {
        return (success ? "成功" : "失败") + "完成任务: " + message;
    }
} 
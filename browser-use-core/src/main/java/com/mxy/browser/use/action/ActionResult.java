package com.mxy.browser.use.action;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 动作执行结果
 * 保存动作执行的成功/失败状态和消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionResult {
    
    /**
     * 执行是否成功
     */
    private boolean success;
    
    /**
     * 执行结果消息
     */
    private String message;
    
    /**
     * 创建成功结果
     * 
     * @param message 成功消息
     * @return 成功结果
     */
    public static ActionResult success(String message) {
        return new ActionResult(true, message);
    }
    
    /**
     * 创建失败结果
     * 
     * @param message 失败消息
     * @return 失败结果
     */
    public static ActionResult error(String message) {
        return new ActionResult(false, message);
    }
} 
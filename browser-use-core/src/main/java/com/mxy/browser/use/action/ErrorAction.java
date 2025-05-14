package com.mxy.browser.use.action;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 错误动作
 * 用于表示解析或执行过程中的错误
 */
@Data
@AllArgsConstructor
public class ErrorAction implements Action {
    
    /**
     * 错误消息
     */
    private String message;
    
    @Override
    public String getType() {
        return "error";
    }
    
    @Override
    public String getDescription() {
        return "错误: " + message;
    }
} 
package com.mxy.browser.use.action;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 等待动作
 * 用于等待一段时间
 */
@Data
@AllArgsConstructor
public class WaitAction implements Action {
    
    /**
     * 等待秒数
     */
    private int seconds;
    
    @Override
    public String getType() {
        return "wait";
    }
    
    @Override
    public String getDescription() {
        return "等待 " + seconds + " 秒";
    }
} 
package com.mxy.browser.use.action;

import lombok.Data;

/**
 * 前进到下一页动作
 */
@Data
public class ForwardAction implements Action {
    
    @Override
    public String getType() {
        return "forward";
    }
    
    @Override
    public String getDescription() {
        return "前进到下一页";
    }
} 
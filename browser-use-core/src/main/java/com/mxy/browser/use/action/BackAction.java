package com.mxy.browser.use.action;

import lombok.Data;

/**
 * 返回上一页动作
 */
@Data
public class BackAction implements Action {
    
    @Override
    public String getType() {
        return "back";
    }
    
    @Override
    public String getDescription() {
        return "返回上一页";
    }
} 
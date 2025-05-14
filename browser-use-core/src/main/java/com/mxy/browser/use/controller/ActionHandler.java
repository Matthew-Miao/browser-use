package com.mxy.browser.use.controller;

import com.mxy.browser.use.action.Action;
import com.mxy.browser.use.action.ActionResult;
import com.mxy.browser.use.browser.BrowserContext;

/**
 * 动作处理器接口
 * 用于处理特定类型的动作
 */
@FunctionalInterface
public interface ActionHandler {
    
    /**
     * 处理动作
     * 
     * @param action 要处理的动作
     * @param browserContext 浏览器上下文
     * @return 处理结果
     */
    ActionResult handle(Action action, BrowserContext browserContext);
} 
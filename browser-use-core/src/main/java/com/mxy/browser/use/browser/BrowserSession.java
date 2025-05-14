package com.mxy.browser.use.browser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 浏览器会话
 * 保存浏览器会话状态
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrowserSession {
    
    /**
     * Playwright浏览器上下文
     */
    private com.microsoft.playwright.BrowserContext playwrightContext;
    
    /**
     * 浏览器状态
     */
    private BrowserState cachedState;
}
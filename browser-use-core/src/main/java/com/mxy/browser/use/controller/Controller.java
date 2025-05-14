package com.mxy.browser.use.controller;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.NavigateOptions;
import com.microsoft.playwright.Page.WaitForSelectorOptions;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.mxy.browser.use.action.*;
import com.mxy.browser.use.browser.BrowserContext;
import com.mxy.browser.use.browser.BrowserState;
import com.mxy.browser.use.dom.DomElement;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

/**
 * 控制器
 * 负责将动作转换为浏览器操作并执行
 */
@Slf4j
public class Controller {

    /**
     * 动作处理器映射
     */
    private final Map<String, BiFunction<Action, BrowserContext, ActionResult>> handlers = new HashMap<>();
    
    /**
     * 创建控制器
     */
    public Controller() {
        registerHandlers();
    }
    
    /**
     * 注册动作处理器
     */
    private void registerHandlers() {
        handlers.put("click", this::handleClickAction);
        handlers.put("type", this::handleTypeAction);
        handlers.put("navigate", this::handleNavigateAction);
        handlers.put("wait", this::handleWaitAction);
        handlers.put("done", (action, context) -> new ActionResult(true, "任务完成"));
    }
    
    /**
     * 执行动作
     * 
     * @param action 要执行的动作
     * @param browserContext 浏览器上下文
     * @return 执行结果
     */
    public CompletableFuture<ActionResult> executeAction(Action action, BrowserContext browserContext) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("执行动作: {}", action.getDescription());
                
                String actionType = action.getType();
                BiFunction<Action, BrowserContext, ActionResult> handler = handlers.get(actionType);
                
                if (handler == null) {
                    return new ActionResult(false, "未知的动作类型: " + actionType);
                }
                
                return handler.apply(action, browserContext);
            } catch (Exception e) {
                log.error("执行动作失败", e);
                return new ActionResult(false, "执行失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 处理点击动作
     */
    private ActionResult handleClickAction(Action action, BrowserContext browserContext) {
        try {
            ClickAction clickAction = (ClickAction) action;
            int index = clickAction.getIndex();
            
            // 获取当前页面状态
            BrowserState state = browserContext.getSession().get().getCachedState();
            
            // 获取元素信息
            DomElement element = state.getSelectorMap().get(index);
            if (element == null) {
                return new ActionResult(false, "未找到索引为 " + index + " 的元素");
            }
            
            // 获取当前页面
            Page page = browserContext.getCurrentPage();
            
            // 查找元素
            String selector = buildSelector(element);
            
            // 等待元素可见
            page.waitForSelector(selector, new WaitForSelectorOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(5000));
            
            // 滚动到元素位置
            page.evaluate("selector => document.querySelector(selector)?.scrollIntoView({behavior: 'smooth', block: 'center'})", selector);
            
            // 高亮元素（可选）
            highlightElement(page, selector);
            
            // 延迟一小段时间，模拟人类行为
            Thread.sleep((long) (Math.random() * 500 + 300));
            
            // 点击元素
            page.click(selector, new Page.ClickOptions()
                    .setDelay(50)
                    .setButton(MouseButton.LEFT)
                    .setForce(false));
            
            // 等待页面加载
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            
            return new ActionResult(true, "成功点击元素");
        } catch (Exception e) {
            log.error("点击操作失败", e);
            return new ActionResult(false, "点击失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理输入文本动作
     */
    private ActionResult handleTypeAction(Action action, BrowserContext browserContext) {
        try {
            TypeAction typeAction = (TypeAction) action;
            int index = typeAction.getIndex();
            String text = typeAction.getText();
            
            // 获取当前页面状态
            BrowserState state = browserContext.getSession().get().getCachedState();
            
            // 获取元素信息
            DomElement element = state.getSelectorMap().get(index);
            if (element == null) {
                return new ActionResult(false, "未找到索引为 " + index + " 的元素");
            }
            
            // 获取当前页面
            Page page = browserContext.getCurrentPage();
            
            // 查找元素
            String selector = buildSelector(element);
            
            // 等待元素可见
            page.waitForSelector(selector, new WaitForSelectorOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(5000));
            
            // 滚动到元素位置
            page.evaluate("selector => document.querySelector(selector)?.scrollIntoView({behavior: 'smooth', block: 'center'})", selector);
            
            // 高亮元素（可选）
            highlightElement(page, selector);
            
            // 清除现有文本（三击全选）
            page.click(selector, new Page.ClickOptions().setClickCount(3));
            
            // 等待一小段时间
            Thread.sleep(300);
            
            // 输入新文本
            page.type(selector, text, new Page.TypeOptions().setDelay(50));
            
            return new ActionResult(true, "成功输入文本: " + text);
        } catch (Exception e) {
            log.error("输入操作失败", e);
            return new ActionResult(false, "输入失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理导航动作
     */
    private ActionResult handleNavigateAction(Action action, BrowserContext browserContext) {
        try {
            NavigateAction navigateAction = (NavigateAction) action;
            String url = navigateAction.getUrl();
            
            // 规范化URL
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            
            // 获取当前页面
            Page page = browserContext.getCurrentPage();
            
            // 导航到URL
            Response response = page.navigate(url, new NavigateOptions()
                    .setTimeout(30000));
            
            // 等待页面加载
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            
            if (response == null) {
                return new ActionResult(false, "导航无响应");
            }
            
            // 检查响应状态
            int status = response.status();
            if (status >= 400) {
                return new ActionResult(false, "导航失败，HTTP状态码: " + status);
            }
            
            return new ActionResult(true, "成功导航到: " + url);
        } catch (Exception e) {
            log.error("导航操作失败", e);
            return new ActionResult(false, "导航失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理等待动作
     */
    private ActionResult handleWaitAction(Action action, BrowserContext browserContext) {
        try {
            WaitAction waitAction = (WaitAction) action;
            int seconds = waitAction.getSeconds();
            
            // 限制最大等待时间
            if (seconds > 60) {
                seconds = 60;
            }
            
            // 等待指定的时间
            Thread.sleep(seconds * 1000L);
            
            return new ActionResult(true, "等待完成: " + seconds + "秒");
        } catch (Exception e) {
            log.error("等待操作失败", e);
            return new ActionResult(false, "等待失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建元素选择器
     */
    private String buildSelector(DomElement element) {
        // 尝试使用ID选择器
        if (element.getId() != null && !element.getId().isEmpty()) {
            return "#" + element.getId();
        }
        
        // 如果没有ID，使用XPath
        return "xpath=" + element.getXpath();
    }
    
    /**
     * 高亮显示元素
     */
    private void highlightElement(Page page, String selector) {
        try {
            // 注入高亮样式
            page.evaluate("selector => {\n" +
                    "  const el = document.querySelector(selector) || document.evaluate(selector.replace(/^xpath=/, ''), document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;\n" +
                    "  if (!el) return;\n" +
                    "  const oldOutline = el.style.outline;\n" +
                    "  const oldZIndex = el.style.zIndex;\n" +
                    "  const oldPosition = el.style.position;\n" +
                    "  \n" +
                    "  el.style.outline = '2px solid red';\n" +
                    "  el.style.zIndex = '10000';\n" +
                    "  if (getComputedStyle(el).position === 'static') {\n" +
                    "    el.style.position = 'relative';\n" +
                    "  }\n" +
                    "  \n" +
                    "  // 3秒后移除高亮\n" +
                    "  setTimeout(() => {\n" +
                    "    el.style.outline = oldOutline;\n" +
                    "    el.style.zIndex = oldZIndex;\n" +
                    "    el.style.position = oldPosition;\n" +
                    "  }, 3000);\n" +
                    "}", selector);
        } catch (Exception e) {
            // 高亮失败不影响主要功能，仅记录日志
            log.warn("元素高亮失败: {}", e.getMessage());
        }
    }
} 
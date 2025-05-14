package com.mxy.browser.use.dom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * DOM服务
 * 负责页面DOM树的解析和处理
 */
@Slf4j
public class DomService {

    /**
     * Playwright页面对象
     */
    private final Page page;
    
    /**
     * JSON处理器
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 构建DOM树的JavaScript代码
     */
    private final String buildDomTreeJs;

    /**
     * 创建DOM服务
     * 
     * @param page Playwright页面对象
     */
    public DomService(Page page) {
        this.page = page;
        this.buildDomTreeJs = loadBuildDomTreeJs();
    }

    /**
     * 加载构建DOM树的JavaScript
     * 
     * @return JavaScript代码
     */
    private String loadBuildDomTreeJs() {
        try {
            // 从资源目录加载JavaScript文件
            ClassPathResource resource = new ClassPathResource("js/buildDomTree.js");
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("无法加载buildDomTree.js", e);
            throw new RuntimeException("无法加载buildDomTree.js", e);
        }
    }

    /**
     * 获取页面的可点击元素
     * 
     * @param highlightElements 是否高亮元素
     * @param focusElement 焦点元素索引
     * @param viewportExpansion 视口扩展像素
     * @return DOM状态
     */
    public CompletableFuture<DomState> getClickableElements(
            boolean highlightElements,
            int focusElement,
            int viewportExpansion) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return buildDomTree(highlightElements, focusElement, viewportExpansion).get();
            } catch (InterruptedException | ExecutionException e) {
                log.error("获取可点击元素失败", e);
                throw new RuntimeException("获取可点击元素失败", e);
            }
        });
    }

    /**
     * 获取跨域iframe的URL列表
     * 
     * @return iframe URL列表的CompletableFuture
     */
    public CompletableFuture<List<String>> getCrossOriginIframes() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 找出所有隐藏的iframe (通常用于广告和跟踪)
                Object o = page.locator("iframe")
                        .filter(new Locator.FilterOptions().setHasText(""))
                        .evaluateAll("e => e.map(e => e.src)", String[].class);
                List<String> hiddenFrameUrls = new ArrayList<>();
                if (o instanceof String[]) {
                    hiddenFrameUrls.addAll(List.of((String[]) o));
                }
                
                List<String> crossOriginIframes = new ArrayList<>();
                if (page.frames() != null) {
                    String pageHostname = getHostname(page.url());
                    
                    for (com.microsoft.playwright.Frame frame : page.frames()) {
                        String frameUrl = frame.url();
                        String frameHostname = getHostname(frameUrl);
                        
                        // 排除data:urls和about:blank
                        if (frameHostname.isEmpty()) continue;
                        
                        // 排除同源iframe
                        if (frameHostname.equals(pageHostname)) continue;
                        
                        // 排除隐藏的iframe
                        if (hiddenFrameUrls.contains(frameUrl)) continue;
                        
                        // 排除常见广告网络的追踪iframe
                        if (isAdUrl(frameUrl)) continue;
                        
                        crossOriginIframes.add(frameUrl);
                    }
                }
                
                return crossOriginIframes;
            } catch (Exception e) {
                log.error("获取跨域iframe失败", e);
                throw new RuntimeException("获取跨域iframe失败", e);
            }
        });
    }

    /**
     * 获取URL的主机名
     * 
     * @param url URL
     * @return 主机名
     */
    private String getHostname(String url) {
        if (url == null || url.isEmpty() || 
                url.startsWith("data:") || url.startsWith("about:") ||
                !url.contains("://")) {
            return "";
        }
        
        try {
            String withoutProtocol = url.split("://")[1];
            return withoutProtocol.split("/")[0];
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 检查URL是否为广告URL
     * 
     * @param url 要检查的URL
     * @return 是否为广告URL
     */
    private boolean isAdUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        
        String hostname = getHostname(url);
        return hostname.contains("doubleclick.net") ||
               hostname.contains("adroll.com") ||
               hostname.contains("googletagmanager.com");
    }

    /**
     * 构建DOM树
     * 
     * @param highlightElements 是否高亮元素
     * @param focusElement 焦点元素索引
     * @param viewportExpansion 视口扩展像素
     * @return DOM状态
     */
    private CompletableFuture<DomState> buildDomTree(
            boolean highlightElements,
            int focusElement,
            int viewportExpansion) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("构建DOM树");
                
                if ("about:blank".equals(page.url())) {
                    // 如果页面是空白页，返回空DOM树
                    DomElement emptyRoot = DomElement.builder()
                            .tagName("body")
                            .xpath("")
                            .isVisible(false)
                            .build();
                    return new DomState(emptyRoot, new HashMap<>());
                }
                
                // 检查页面是否能正确执行JavaScript
                if (page.evaluate("1+1").equals(2)) {
                    // 准备JavaScript执行参数
                    Map<String, Object> args = new HashMap<>();
                    args.put("doHighlightElements", highlightElements);
                    args.put("focusHighlightIndex", focusElement);
                    args.put("viewportExpansion", viewportExpansion);
                    args.put("debugMode", log.isDebugEnabled());
                    
                    // 执行DOM树构建脚本
                    String resultJson = page.evaluate(buildDomTreeJs, args).toString();
                    
                    // 解析返回的JSON
                    JsonNode evalPage = objectMapper.readTree(resultJson);
                    
                    // 如果开启了调试，记录性能指标
                    if (log.isDebugEnabled() && evalPage.has("perfMetrics")) {
                        log.debug("DOM树构建性能指标: {}", 
                                objectMapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(evalPage.get("perfMetrics")));
                    }
                    
                    // 构建DOM树
                    return constructDomTree(evalPage);
                } else {
                    throw new RuntimeException("页面无法正确执行JavaScript");
                }
            } catch (Exception e) {
                log.error("构建DOM树失败", e);
                throw new RuntimeException("构建DOM树失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 根据JavaScript返回的数据构建DOM树
     * 
     * @param evalPage JavaScript返回的数据
     * @return DOM状态
     */
    private DomState constructDomTree(JsonNode evalPage) throws IOException {
        Map<String, DomNode> nodeMap = new HashMap<>();
        Map<Integer, DomElement> selectorMap = new HashMap<>();
        
        JsonNode jsNodeMap = evalPage.get("map");
        String jsRootId = evalPage.get("rootId").asText();
        
        // 第一遍：解析所有节点并创建节点实例
        jsNodeMap.fields().forEachRemaining(entry -> {
            String id = entry.getKey();
            JsonNode nodeData = entry.getValue();
            
            DomNode node = parseNode(nodeData);
            if (node != null) {
                nodeMap.put(id, node);
                
                // 如果是元素节点且有高亮索引，添加到选择器映射
                if (node instanceof DomElement) {
                    DomElement elementNode = (DomElement) node;
                    Integer highlightIndex = elementNode.getHighlightIndex();
                    if (highlightIndex != null) {
                        selectorMap.put(highlightIndex, elementNode);
                    }
                }
            }
        });
        
        // 第二遍：建立节点间的父子关系
        jsNodeMap.fields().forEachRemaining(entry -> {
            String id = entry.getKey();
            JsonNode nodeData = entry.getValue();
            
            if (nodeData.has("children") && nodeMap.containsKey(id) && nodeMap.get(id) instanceof DomElement) {
                DomElement parentNode = (DomElement) nodeMap.get(id);
                
                // 添加子节点
                nodeData.get("children").forEach(childId -> {
                    String childIdStr = childId.asText();
                    if (nodeMap.containsKey(childIdStr)) {
                        DomNode childNode = nodeMap.get(childIdStr);
                        childNode.setParent(parentNode);
                        parentNode.getChildren().add(childNode);
                    }
                });
            }
        });
        
        // 获取根节点
        DomElement rootNode = (DomElement) nodeMap.get(jsRootId);
        if (rootNode == null) {
            throw new IOException("无法构建DOM树：找不到根节点");
        }
        
        return new DomState(rootNode, selectorMap);
    }

    /**
     * 解析单个节点数据
     * 
     * @param nodeData 节点数据
     * @return DOM节点
     */
    private DomNode parseNode(JsonNode nodeData) {
        if (nodeData == null || nodeData.isEmpty()) {
            return null;
        }
        
        // 处理文本节点
        if (nodeData.has("type") && "TEXT_NODE".equals(nodeData.get("type").asText())) {
            return DomTextNode.builder()
                    .text(nodeData.get("text").asText())
                    .isVisible(nodeData.get("isVisible").asBoolean())
                    .build();
        }
        
        // 处理元素节点
        ViewportInfo viewportInfo = null;
        if (nodeData.has("viewport")) {
            viewportInfo = ViewportInfo.builder()
                    .width(nodeData.get("viewport").get("width").asInt())
                    .height(nodeData.get("viewport").get("height").asInt())
                    .build();
        }
        
        // 构建属性映射
        Map<String, String> attributes = new HashMap<>();
        if (nodeData.has("attributes")) {
            nodeData.get("attributes").fields().forEachRemaining(attr -> 
                attributes.put(attr.getKey(), attr.getValue().asText()));
        }
       return DomElement.builder()
                .tagName(nodeData.get("tagName").asText())
                .xpath(nodeData.get("xpath").asText())
                .attributes(attributes)
                .children(new ArrayList<>())
                .isVisible(nodeData.has("isVisible") ? nodeData.get("isVisible").asBoolean() : false)
                .isInteractive(nodeData.has("isInteractive") ? nodeData.get("isInteractive").asBoolean() : false)
                .isTopElement(nodeData.has("isTopElement") ? nodeData.get("isTopElement").asBoolean() : false)
                .isInViewport(nodeData.has("isInViewport") ? nodeData.get("isInViewport").asBoolean() : false)
                .highlightIndex(nodeData.has("highlightIndex") ? nodeData.get("highlightIndex").asInt() : null)
                .shadowRoot(nodeData.has("shadowRoot") ? nodeData.get("shadowRoot").asBoolean() : false)
                .viewportInfo(viewportInfo)
                .build();

    }
} 
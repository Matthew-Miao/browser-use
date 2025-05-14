package com.mxy.browser.use.dom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DOM元素节点
 * 表示页面中的一个HTML元素
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomElement implements DomNode{
    
    /**
     * 元素标签名称
     */
    private String tagName;
    
    /**
     * XPath路径
     */
    private String xpath;
    
    /**
     * 元素属性
     */
    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();
    
    /**
     * 子元素列表
     */
    @Builder.Default
    private List<DomNode> children = new ArrayList<>();
    
    /**
     * 父元素
     */
    private DomElement parent;
    
    /**
     * 元素是否可见
     */
    private boolean isVisible;
    
    /**
     * 元素是否可交互
     */
    private boolean isInteractive;
    
    /**
     * 元素是否为顶层元素
     */
    private boolean isTopElement;
    
    /**
     * 元素是否在视口内
     */
    private boolean isInViewport;
    
    /**
     * 高亮索引，用于在UI中标识和后续交互
     */
    private Integer highlightIndex;
    
    /**
     * 元素是否有Shadow DOM
     */
    private boolean shadowRoot;
    
    /**
     * 视口信息
     */
    private ViewportInfo viewportInfo;
    
    /**
     * 获取元素ID (如果有)
     * 
     * @return 元素ID或null
     */
    public String getId() {
        return attributes.get("id");
    }
    
    /**
     * 获取元素类名 (如果有)
     * 
     * @return 元素类名或null
     */
    public String getClassName() {
        return attributes.get("class");
    }
    
    /**
     * 获取元素名称 (如果有)
     * 
     * @return 元素名称或null
     */
    public String getName() {
        return attributes.get("name");
    }
    
    /**
     * 获取元素类型 (如果有)
     * 
     * @return 元素类型或null
     */
    public String getType() {
        return attributes.get("type");
    }
    
    /**
     * 获取元素值 (如果有)
     * 
     * @return 元素值或null
     */
    public String getValue() {
        return attributes.get("value");
    }
    
    /**
     * 获取元素ARIA标签 (如果有)
     * 
     * @return 元素ARIA标签或null
     */
    public String getAriaLabel() {
        return attributes.get("aria-label");
    }
    
    /**
     * 获取元素占位符 (如果有)
     * 
     * @return 元素占位符或null
     */
    public String getPlaceholder() {
        return attributes.get("placeholder");
    }
    
    /**
     * 获取元素内的所有文本
     * 
     * @return 元素内的所有文本
     */
    public String getAllText() {
        StringBuilder sb = new StringBuilder();
        getAllTextRecursive(sb);
        return sb.toString().trim();
    }
    
    /**
     * 递归获取元素内的所有文本
     * 
     * @param sb 存储文本的StringBuilder
     */
    private void getAllTextRecursive(StringBuilder sb) {
        for (DomNode child : children) {
            if (child instanceof DomTextNode) {
                DomTextNode textNode = (DomTextNode) child;
                if (textNode.isVisible()) {
                    sb.append(textNode.getText()).append(" ");
                }
            } else if (child instanceof DomElement) {
                ((DomElement) child).getAllTextRecursive(sb);
            }
        }
    }
    
    /**
     * 获取下一个可点击元素前的所有文本
     * 
     * @param maxDepth 最大深度
     * @return 文本内容
     */
    public String getAllTextTillNextClickableElement(int maxDepth) {
        StringBuilder sb = new StringBuilder();
        getAllTextTillNextClickableElementRecursive(sb, maxDepth, 0);
        return sb.toString().trim();
    }
    
    /**
     * 递归获取下一个可点击元素前的所有文本
     * 
     * @param sb 存储文本的StringBuilder
     * @param maxDepth 最大深度
     * @param currentDepth 当前深度
     */
    private void getAllTextTillNextClickableElementRecursive(StringBuilder sb, int maxDepth, int currentDepth) {
        if (currentDepth > maxDepth) return;
        
        for (DomNode child : children) {
            if (child instanceof DomTextNode) {
                DomTextNode textNode = (DomTextNode) child;
                if (textNode.isVisible()) {
                    sb.append(textNode.getText()).append(" ");
                }
            } else if (child instanceof DomElement) {
                DomElement elementNode = (DomElement) child;
                if (elementNode.isInteractive() && elementNode.isVisible() && elementNode != this) {
                    // 遇到另一个可交互元素时停止
                    break;
                }
                elementNode.getAllTextTillNextClickableElementRecursive(sb, maxDepth, currentDepth + 1);
            }
        }
    }
    
    /**
     * 检查元素是否具有指定的文本内容
     * 
     * @param text 要查找的文本
     * @return 是否包含该文本
     */
    public boolean containsText(String text) {
        if (text == null || text.isEmpty()) return false;
        
        String allText = getAllText().toLowerCase();
        return allText.contains(text.toLowerCase());
    }
    
    /**
     * 查找包含指定文本的子元素
     * 
     * @param text 要查找的文本
     * @return 包含文本的元素列表
     */
    public List<DomElement> findElementsWithText(String text) {
        List<DomElement> result = new ArrayList<>();
        findElementsWithTextRecursive(text, result);
        return result;
    }
    
    /**
     * 递归查找包含指定文本的子元素
     * 
     * @param text 要查找的文本
     * @param result 结果列表
     */
    private void findElementsWithTextRecursive(String text, List<DomElement> result) {
        if (containsText(text)) {
            result.add(this);
        }
        
        for (DomNode child : children) {
            if (child instanceof DomElement) {
                ((DomElement) child).findElementsWithTextRecursive(text, result);
            }
        }
    }
    
    /**
     * 获取元素简短描述
     * 
     * @return 元素描述
     */
    public String getShortDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s", tagName));
        
        if (highlightIndex != null) {
            sb.append(String.format(" [%d]", highlightIndex));
        }
        
        String text = getAllText();
        if (text != null && !text.isBlank()) {
            if (text.length() > 50) {
                sb.append(String.format(" \"%s...\"", text.substring(0, 47)));
            } else {
                sb.append(String.format(" \"%s\"", text));
            }
        }
        
        String id = getId();
        if (id != null) {
            sb.append(String.format(" (id=%s)", id));
        }
        
        return sb.toString();
    }
} 
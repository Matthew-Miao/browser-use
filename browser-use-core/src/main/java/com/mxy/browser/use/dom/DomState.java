package com.mxy.browser.use.dom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DOM状态
 * 包含DOM树和元素选择器映射
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomState {
    
    /**
     * DOM树根节点
     */
    private DomElement elementTree;
    
    /**
     * 元素选择器映射
     * 索引 -> DOM元素
     */
    private Map<Integer, DomElement> selectorMap;
} 
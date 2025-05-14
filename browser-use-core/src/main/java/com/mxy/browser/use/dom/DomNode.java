package com.mxy.browser.use.dom;

/**
 * DOM节点接口
 * 表示DOM树中的一个节点
 */
public interface DomNode {
    
    /**
     * 获取节点是否可见
     * 
     * @return 节点是否可见
     */
    boolean isVisible();
    
    /**
     * 获取父节点
     * 
     * @return 父节点
     */
    DomElement getParent();
    
    /**
     * 设置父节点
     * 
     * @param parent 父节点
     */
    void setParent(DomElement parent);
} 
package com.mxy.browser.use.browser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 浏览器代理设置
 * 对应Python版本的ProxySettings类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProxySettings {

    /**
     * 代理服务器地址，例如: http://myproxy.com:3128 或 socks5://myproxy.com:3128
     */
    private String server;
    
    /**
     * 应该跳过代理的地址，例如: ".googleapis.com,.google.com"
     */
    private String bypass;
    
    /**
     * 代理服务器的用户名
     */
    private String username;
    
    /**
     * 代理服务器的密码
     */
    private String password;
} 
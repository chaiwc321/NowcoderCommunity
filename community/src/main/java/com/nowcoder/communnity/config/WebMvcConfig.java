package com.nowcoder.communnity.config;

import com.nowcoder.communnity.controller.interceptor.LoginRequiredInterceptor;
import com.nowcoder.communnity.controller.interceptor.LoginTicketInterceptor;
import com.nowcoder.communnity.controller.interceptor.MessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;

    @Autowired
    private LoginRequiredInterceptor loginRequiredInterceptor;

    @Autowired
    private MessageInterceptor messageInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/**/*.css", "**/*.png", "**/*.jpg", "**/*.jpeg", "**/*.js");
        registry.addInterceptor(loginRequiredInterceptor)
                .excludePathPatterns("/**/*.css", "**/*.png", "**/*.jpg", "**/*.jpeg", "**/*.js");
        registry.addInterceptor(messageInterceptor)
                .excludePathPatterns("/**/*.css", "**/*.png", "**/*.jpg", "**/*.jpeg", "**/*.js");
    }
}

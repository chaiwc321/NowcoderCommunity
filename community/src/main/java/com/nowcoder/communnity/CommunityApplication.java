package com.nowcoder.communnity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class CommunityApplication {
	// 注解管理bean的周期，通常是初始化方法
	@PostConstruct
	public void init() {
		// 解决netty启动冲突问题
		// see netty4utils.setAvailableProcessor
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	}

	public static void main(String[] args) {
		SpringApplication.run(CommunityApplication.class, args);
	}

}

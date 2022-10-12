package com.nowcoder.communnity;

import com.nowcoder.communnity.alphadata.AlphaDao;
import com.nowcoder.communnity.alphaservice.AlphaService;
import javafx.application.Application;
import org.junit.Test;
//import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)  //test中可以以main中为配置类
public class CommunityApplicationTests implements ApplicationContextAware {

	// 定义私有成员容器变量
	private ApplicationContext applicationContext;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext; // 重写接口函数，指定容器
	}

	// 测试容器
	@Test
	public void testApplication(){
		System.out.println(applicationContext);

		// 通过容器得到dao Bean
		AlphaDao alphaDao = applicationContext.getBean(AlphaDao.class);
		System.out.println(alphaDao);
	}

	// 测试容器如何管理Beans
	@Test
	public void beansManagement(){
		// 获得服务bean
		AlphaService alphaService = applicationContext.getBean(AlphaService.class);
		System.out.println(alphaService);  // 打印服务bean
		// 还可以测试单例还是多例
		// alphaService = applicationContext.getBean(AlphaService.class);
	}
}

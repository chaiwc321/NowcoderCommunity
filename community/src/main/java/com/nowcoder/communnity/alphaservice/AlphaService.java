package com.nowcoder.communnity.alphaservice;


import com.nowcoder.communnity.alphadata.AlphaDao;
import com.nowcoder.communnity.alphadata.AlphaDaoMyBatis;
import com.nowcoder.communnity.alphadata.DiscussPostMapper;
import com.nowcoder.communnity.alphadata.UserMapper;
import com.nowcoder.communnity.entity.User;
import com.nowcoder.communnity.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class AlphaService {
    // 实例化
//    public AlphaService() {
//        System.out.println("实例化AlphaService");
//    }
//
//    // 初始化
//    @PostConstruct
//    public void init(){
//        System.out.println("初始化AlphaService");
//    }
//
//    // 销毁
//    @PreDestroy
//    public void destroy(){
//        System.out.println("销毁AlphaService");
//    }
//
//    // 定义数据成员变量
//    @Autowired
//    private AlphaDao alphaDao;
    @Autowired
    private AlphaDaoMyBatis alphaDaoMyBatis;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    // 定义一个方法
    public String find(){
        return alphaDaoMyBatis.select();
    }

    @Transactional(isolation = Isolation.DEFAULT, propagation = Propagation.REQUIRED)
    public Object save1() {

        User user = new User();
        user.setUsername("alpha");
        user.setSalt(CommunityUtil.generateUUID().substring(0,4));
        return "dfsa";


    }


}

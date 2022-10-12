package com.nowcoder.communnity;

import com.nowcoder.communnity.alphadata.DiscussPostMapper;
import com.nowcoder.communnity.alphadata.UserMapper;
import com.nowcoder.communnity.entity.DiscussPost;
import com.nowcoder.communnity.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTest {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Test
    public void testSelect(){
        User user = userMapper.selectById(102);
        System.out.println(user);
        user = userMapper.selectByName("柴文超");
        System.out.println(user);
        user = userMapper.selectByEmail("nowcoder102@sina.com");
        System.out.println(user);
    }
    @Test
    public void testInsert(){
        User user = new User();
        user.setUsername("柴文超");
        user.setEmail("768735936@qq.com");
        user.setPassword("768735936a");
        user.setHeaderUrl("http://images.nowcoder.com/head/200t.png");
        user.setSalt("12345");
        user.setCreateTime(new Date());
        int row = userMapper.insertUser(user);
        System.out.println(row);
    }
    @Test
    public void testUpdate(){
        int r = 0;
        r = userMapper.updateStatus(150, 1);
        r = userMapper.updateHeader(150,"www.fuck.com");
        r = userMapper.updatePassword(150, "cec123");
        System.out.println(r);
        User user = userMapper.selectById(150);
        System.out.println(user);
    }
    @Test
    public void testSelectDiscussPost(){
        List<DiscussPost> discussPostList = discussPostMapper.selectDiscussPosts(149, 0, 10);
        for (DiscussPost discussP:discussPostList
             ) {
            System.out.println(discussP);
        }
    }
    @Test
    public void testDiscussRows(){
        int r = discussPostMapper.selectDiscussPostRows(149);
        System.out.println(r);
    }
}

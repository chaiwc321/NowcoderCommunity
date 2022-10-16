package com.nowcoder.communnity.alphaservice;

import com.nowcoder.communnity.alphadata.UserMapper;
import com.nowcoder.communnity.entity.User;
import com.nowcoder.communnity.util.CommunityConstant;
import com.nowcoder.communnity.util.CommunityUtil;
import com.nowcoder.communnity.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User findUser(int Id){
        return userMapper.selectById(Id);
    }

    // 根据用户注册时提交的信息，查找数据库并返回对应信息，用map保存
    public Map<String, Object> register(User user){

        Map<String, Object> map = new HashMap<>();

        if(user == null){
            throw new IllegalArgumentException("参数不能为空！");
        }

        if(StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg", "账号名不能为空！");
            return map;
        }

        if(StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }

        if(StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg", "邮箱不能为空！");
            return map;
        }

        // 验证信息
        User u = userMapper.selectByName(user.getUsername());
        if(u != null){
            map.put("usernameMsg", "用户名已存在");
            return map;
        }

        u = userMapper.selectByEmail(user.getEmail());
        if(u != null){
            map.put("emailMsg", "邮箱已存在");
            return map;
        }

        // 添加用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setType(0);
        user.setStatus(0);
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000))); // 生成1000以内的随机数
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 激活邮箱
        Context context = new Context();
        context.setVariable("Email", user.getEmail());  // 设置邮件html中的值
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("Url", url); // 设置邮件html中的值
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }

    public int activateStatus(int userId, String code){

        User user = userMapper.selectById(userId);
        if(user.getStatus()== 1){
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){
            userMapper.updateStatus(userId, 1);
            return ACTIVATION_SUCCESS;
        }else {
            return ACTIVATION_FAILURE;
        }
    }
}

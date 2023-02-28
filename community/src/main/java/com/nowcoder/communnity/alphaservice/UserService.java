package com.nowcoder.communnity.alphaservice;

import com.nowcoder.communnity.alphadata.LoginTicketMapper;
import com.nowcoder.communnity.alphadata.UserMapper;
import com.nowcoder.communnity.entity.LoginTicket;
import com.nowcoder.communnity.entity.User;
import com.nowcoder.communnity.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User findUser(int Id){
//        return userMapper.selectById(Id);
        User user = getCache(Id);
        if(user == null) {
            user = initCache(Id);
        }
        return user;
    }


    public User findUserByName(String username) {
        return userMapper.selectByName(username);
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
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        }else {
            return ACTIVATION_FAILURE;
        }
    }

    public Map<String, Object> setPassword(User user, String password, String curPassword, String finalPassword) {

        HashMap<String, Object> map = new HashMap<>();
        String prePassword = user.getPassword();

        if(StringUtils.isBlank(password) || StringUtils.isBlank(curPassword) || StringUtils.isBlank(finalPassword)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        password = CommunityUtil.md5(password + user.getSalt());
        if (!prePassword.equals(password)){
            map.put("passwordMsg", "密码不正确！");
            return map;
        }
        if(!curPassword.equals(finalPassword)) {
            map.put("passwordMsg", "两次密码不一致");
            return map;
        }
        if(curPassword.length() < 6) {
            map.put("passwordMsg", "密码长度不能小于6位");
            return map;
        }

        curPassword = CommunityUtil.md5(curPassword + user.getSalt());
        if(curPassword.equals(prePassword)) {
            map.put("passwordMsg", "需要和原先密码不一致");
            return map;
        }

        userMapper.updatePassword(user.getId(), curPassword);
        map.put("successMsg", "密码设置成功！请重新登录。");

        return map;

    }

    public Map<String, Object> login(String username, String password, int expiredSeconds) {

        Map<String, Object> map = new HashMap<>();
        // 空值处理
        if(StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }

        if(StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if(user == null){
            map.put("usernameMsg", "该账号不存在！");
            return map;
        }

        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活！");
            return map;
        }

        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)){
            map.put("passwordMsg", "密码不正确！");
            return map;
        }

        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
//        loginTicketMapper.insertLoginTicket(loginTicket);

        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket);

        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    public void logout(String ticket) {
//        loginTicketMapper.updateStatus(ticket,1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);
    }

    public LoginTicket findLoginTicket(String ticket) {
//        return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    public int updateHeader(int userId, String headerUrl) {
//        return userMapper.updateHeader(userId, headerUrl);
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }


    // 1.优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }
    // 2.取不到时初始化缓存数据
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }
    // 3.数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    // 根据用户类型获取其权限
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUser(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });

        return list;
    }

}

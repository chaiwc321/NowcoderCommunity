package com.nowcoder.communnity.controller;

import com.nowcoder.communnity.alphaservice.UserService;
import com.nowcoder.communnity.entity.User;
import com.nowcoder.communnity.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@Controller
public class LoginController implements CommunityConstant {

    @Autowired
    UserService userService;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage(){
        return "/site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() { return "/site/login"; }

    @RequestMapping(path = "/register", method = RequestMethod.POST)  // POST网页的action地址指向此处，这里就可以接收到user
    public String register(Model model, User user){
        Map<String, Object> map = userService.register(user);  // 接收到的user再调用服务层的方法进行处理
        // 表单提交的数据与User类中的属性相匹配，SpringMVC就会自动注入Userr
        if(map == null || map.isEmpty()){    // 没有返回错误信息，因为map中装的错误信息
            model.addAttribute("msg", "系统已经向您的邮箱发送激活邮件，请尽快前往激活！");
            model.addAttribute("target", "/index"); // 稍后跳转到首页的地址
            return "/site/operate-result";  // 注册成功页面
        }else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";  // 返回注册页面
        }
    }

    // http://localhost:8080/community/activation/101/code
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int id, @PathVariable("code") String code){
        int status = userService.activateStatus(id, code);
        if(status ==  ACTIVATION_SUCCESS){
            model.addAttribute("msg", "您以成功激活您的账号!");
            model.addAttribute("target", "/login");
        }else if(status == ACTIVATION_REPEAT){
            model.addAttribute("msg", "无效操作，您的账号已被激活过了！");
            model.addAttribute("target", "/index");
        }else{
            model.addAttribute("msg", "激活失败，您提供的激活码不正确");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

}

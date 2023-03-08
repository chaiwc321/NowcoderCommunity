package com.nowcoder.communnity.controller;

import com.nowcoder.communnity.alphaservice.FollowService;
import com.nowcoder.communnity.alphaservice.LikeService;
import com.nowcoder.communnity.alphaservice.UserService;
import com.nowcoder.communnity.annotation.LoginRequired;
import com.nowcoder.communnity.entity.LoginTicket;
import com.nowcoder.communnity.entity.User;
import com.nowcoder.communnity.util.CommunityConstant;
import com.nowcoder.communnity.util.CommunityUtil;
import com.nowcoder.communnity.util.CookieUtil;
import com.nowcoder.communnity.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.Map;

@Controller
@RequestMapping(path = "/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.domain}")
    private String domain;

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.space.name}")
    private String spaceBucketName;

    @Value("${qiniu.bucket.space.url}")
    private String spaceBucketUrl;


    @LoginRequired
    @RequestMapping(path = "/settingPassword", method = RequestMethod.POST)
    public String settingPassword(Model model, HttpServletRequest request, String password, String curPassword, String finalPassword) {

        String userTicket = CookieUtil.getValue(request, "ticket");
        User user = null;

        if(userTicket != null) {
            LoginTicket loginTicket = userService.findLoginTicket(userTicket);
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                user = userService.findUser(loginTicket.getUserId());
            }
        }

        Map<String, Object> map = userService.setPassword(user, password, curPassword, finalPassword);
        if(map.containsKey("successMsg")) {
            userService.logout(userTicket);
            model.addAttribute("msg", map.get("successMsg"));
            model.addAttribute("target", "/login");
            return "/site/operate-result";
        }else {
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "site/setting";
        }
    }

    // 服务器更新头像
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if(StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空！");
        }
        String url = spaceBucketUrl + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(), url);
        return CommunityUtil.getJSONString(0);
    }


    // 弃用
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {

        if(headerImage == null) {
            model.addAttribute("error", "您还没有选择图片！");
            return "site/setting";
        }

        // 判断后缀名是否正确
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确！");
            return "site/setting";
        }

        // 生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        // 确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败" + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！", e);
        }

        // 更新当前头像web访问路径
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";

    }

    // 弃用
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) throws IOException {

        fileName = uploadPath + "/" + fileName;

        String suffix = fileName.substring(fileName.lastIndexOf("."));

        response.setContentType("image/" + suffix);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
            OutputStream os = response.getOutputStream();
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);  // 直接向网站返回图像
            }
        } catch (IOException e) {
            logger.error("读取头像失败" + e.getMessage());
        } finally {
            fis.close();
        }

    }

    // 个人主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUser(userId);
        if(user == null) {
            throw new RuntimeException("该用户不存在！");
        }

        model.addAttribute("user", user);
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_PERSON);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowCount(ENTITY_TYPE_PERSON, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已经关注
        boolean hasFollowed = false;
        if(hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_PERSON, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);
        return "site/profile";
    }

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getUserSetting(Model model) {
        String fileName = CommunityUtil.generateUUID();

        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0));

        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(spaceBucketName, fileName, 3600, policy);

        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);

        return "site/setting";
    }

}

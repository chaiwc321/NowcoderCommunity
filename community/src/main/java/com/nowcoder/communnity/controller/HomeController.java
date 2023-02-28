package com.nowcoder.communnity.controller;

import com.nowcoder.communnity.alphaservice.DiscussPostService;
import com.nowcoder.communnity.alphaservice.LikeService;
import com.nowcoder.communnity.alphaservice.UserService;
import com.nowcoder.communnity.entity.DiscussPost;
import com.nowcoder.communnity.entity.Page;
import com.nowcoder.communnity.entity.User;
import com.nowcoder.communnity.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController implements CommunityConstant {

    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @RequestMapping(path = "/index", method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page){
        // model和page在连接前就已经被实例化，并且page被自动注入model中，因此不用再addattribute
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index");
        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit());
        ArrayList<Map<String, Object>> discussPosts = new ArrayList<Map<String, Object>>();
        if(list != null){
            for (DiscussPost discussPost:list
                 ) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("post", discussPost);
                User user = userService.findUser(discussPost.getUserId());
                map.put("user", user);

                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPost.getId());
                map.put("likeCount", likeCount);

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        return "/index";
    }

    @RequestMapping(path = "/error", method = RequestMethod.GET)
    public String getErrorPage() {
        return "/error/500";
    }

    @RequestMapping(path = "/denied", method = RequestMethod.GET)
    public String getDeniedPage() {
        return "/error/404";
    }

}

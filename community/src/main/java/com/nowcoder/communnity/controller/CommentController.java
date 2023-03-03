package com.nowcoder.communnity.controller;

import com.nowcoder.communnity.alphaservice.CommentService;
import com.nowcoder.communnity.alphaservice.DiscussPostService;
import com.nowcoder.communnity.annotation.LoginRequired;
import com.nowcoder.communnity.entity.Comment;
import com.nowcoder.communnity.entity.DiscussPost;
import com.nowcoder.communnity.entity.Event;
import com.nowcoder.communnity.entity.User;
import com.nowcoder.communnity.event.EventProducer;
import com.nowcoder.communnity.util.CommunityConstant;
import com.nowcoder.communnity.util.HostHolder;
import com.nowcoder.communnity.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    // 用来找帖子的用户还是id来着
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private RedisTemplate redisTemplate;

    @LoginRequired
    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        User user = hostHolder.getUser();
        if(user == null) {
            return "redirect:/login";
        }
        comment.setUserId(user.getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        // 触发评论事件，系统需要做出响应
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId);

        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);

        // 触发帖子评论事件，将帖子存入es
        if(comment.getEntityType() == ENTITY_TYPE_POST) {
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);
            String redisKey = RedisKeyUtil.getPostScoreKey();
            // Redis什么类型比较好
            redisTemplate.opsForSet().add(redisKey, discussPostId);
        }




        return "redirect:/discuss/detail/" + discussPostId;
    }

}

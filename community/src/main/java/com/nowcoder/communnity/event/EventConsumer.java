package com.nowcoder.communnity.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.communnity.alphaservice.DiscussPostService;
import com.nowcoder.communnity.alphaservice.ElasticsearchService;
import com.nowcoder.communnity.alphaservice.MessageService;
import com.nowcoder.communnity.entity.DiscussPost;
import com.nowcoder.communnity.entity.Event;
import com.nowcoder.communnity.entity.Message;
import com.nowcoder.communnity.util.CommunityConstant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Value("${wk.imgae.storage}")
    private String wkImageStorage;

    @Value("${wk.imgae.command}")
    private String wkImageCommand;

    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_FOLLOW, TOPIC_LIKE})
    public void handCommentMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if (event == null) {
            logger.error("消息格式错误");
            return;
        }

        // 发送站内通知
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());

        if (!event.getData().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                content.put(entry.getKey(), entry.getValue());
            }
        }

        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);

    }

    // 消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if (event == null) {
            logger.error("消息格式错误");
            return;
        }

        // 从事件中得到帖子id，查到对应的帖子，将其存入es
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);

    }

    // 消费发帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if (event == null) {
            logger.error("消息格式错误");
            return;
        }

        elasticsearchService.deleteDiscussPost(event.getEntityId());

    }

    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if (event == null) {
            logger.error("消息格式错误");
            return;
        }

        String htmlUrl = (String)event.getData().get("htmlUrl");
        String fileName = (String)event.getData().get("fileName");
        String suffix = (String)event.getData().get("suffix");

        String cmd = wkImageCommand + "--quality 75" + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功： " + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败: " + e);
        }
    }
}

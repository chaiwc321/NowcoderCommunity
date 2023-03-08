package com.nowcoder.communnity.event;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.communnity.alphaservice.DiscussPostService;
import com.nowcoder.communnity.alphaservice.ElasticsearchService;
import com.nowcoder.communnity.alphaservice.MessageService;
import com.nowcoder.communnity.entity.DiscussPost;
import com.nowcoder.communnity.entity.Event;
import com.nowcoder.communnity.entity.Message;
import com.nowcoder.communnity.util.CommunityConstant;
import com.nowcoder.communnity.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.space.name}")
    private String shareBucketName;

    @Value("${qiniu.bucket.space.url}")
    private String shareBucketUrl;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;  // 定时任务线程池，应该是用quartz

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

        String cmd = wkImageCommand + " --quality 75 " + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功： " + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败: " + e);
        }

        // 图片可能会慢于当前方法的主线程执行，所以要等图片生成后继续执行语句
        // 需要一个定时器 单个服务器定时， 与其他服务器不发生关系
        UploadTask uploadTask = new UploadTask(fileName, suffix);
        Future future = taskScheduler.scheduleAtFixedRate(uploadTask, 500);
        uploadTask.setFuture(future);

    }

    class UploadTask implements Runnable {
        // 调任务时需要知道文件属性
        private String fileName;
        private String suffix;
        // 停止任务需要当前任务的属性 时间 上传次数
        private Future future;
        private long startTime;
        private int  uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            // 停止定时器 两种情况：无法生成 或者 生成后网络出问题导致上传失败
            if(System.currentTimeMillis() - startTime > 30000) {
                logger.error("执行时间过长，终止任务。" + fileName);
                future.cancel(true);
                return;
            }
            if(uploadTimes >= 3) {
                logger.error("上传次数过多，终止任务");
                future.cancel(true);
                return;
            }
            // 本地目录中找到share的文件 判断其是否存在
            // 上传图片 生成上传凭证 指定上传机房（华南）
            // 上传时处理异常 若捕获异常 记录日志（七牛云上传成功率太低会退钱） 重新上传一次
            //

            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if(file.exists()) {
                logger.info(String.format("开始第%d次上传[%s]。", ++uploadTimes, fileName));

                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));
                Auth auth = Auth.create(accessKey, secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
                UploadManager manager = new UploadManager(new Configuration(Zone.zone2()));
                try {
                    Thread.sleep(100000);
                    Response response = manager.put(
                            path, fileName, uploadToken, null, "image/" + suffix, false
                    );

                    JSONObject json = JSONObject.parseObject(response.bodyString());  // 从response里得到的字符串，转为json
                    if(json == null || json.get("code") == null || !json.get("code").toString().equals("0")){
                        logger.info(String.format("第%d次上传失败[%s]", uploadTimes, fileName));
                    }else {
                        logger.info(String.format("第%d次上传成功[%s]", uploadTimes, fileName));
                        future.cancel(true);
                    }

                }catch (QiniuException e) {
                    logger.info(String.format("第%d次上传失败[%s]", uploadTimes, fileName));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }else {
                logger.info("等待图片生成[" + fileName + "].");
            }

        }
    }
}

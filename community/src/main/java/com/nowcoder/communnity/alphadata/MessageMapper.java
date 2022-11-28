package com.nowcoder.communnity.alphadata;

import com.nowcoder.communnity.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {

    // 查询当前用户的会话列表，每个会话返回最新一条私信
    List<Message> selectConversations(int userId, int offset, int limit);
    // 查询会话数量
    int selectConversationCount(int userId);
    // 查询特定会话中的私信
    List<Message> selectLetters(String conversationId, int offset, int limit);
    // 查询私信数量
    int selectLetterCount(String conversationId);
    // 查询未读私信数量（包括特定会话，或全部）
    int selectLetterUnreadCount(int userId, String conversationId);
    // 添加消息
    int insertMessage(Message message);
    // 修改消息的状态
    int updateState(List<Integer> ids, int status);

}

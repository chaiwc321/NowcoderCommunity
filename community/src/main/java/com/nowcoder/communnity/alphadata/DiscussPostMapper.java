package com.nowcoder.communnity.alphadata;

import com.nowcoder.communnity.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit);

    // @Param注解用于给参数取名
    // 如果只有一个参数且在<if>中使用，则必须进行注解
    int selectDiscussPostRows(@Param("userId") int userId);

    int insertDiscussPost(DiscussPost discussPost);

    DiscussPost selectDiscussPostBy(int id);

    int updateCommentCount(int id, int commentCount);
}

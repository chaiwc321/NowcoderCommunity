package com.nowcoder.communnity.alphadata.eslaticsearch;

import com.nowcoder.communnity.entity.DiscussPost;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.data.elasticsearch.repository.ElasticsearchCrudRepository;
import org.springframework.stereotype.Repository;

// 声明对象和类型
@Repository
public interface DiscussPostRepository extends ElasticsearchCrudRepository<DiscussPost, Integer> {
    Page<DiscussPost> search(SearchQuery searchQuery);
}


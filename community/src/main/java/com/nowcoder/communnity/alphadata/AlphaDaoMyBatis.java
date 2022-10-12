package com.nowcoder.communnity.alphadata;


import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class AlphaDaoMyBatis implements AlphaDao{
    @Override
    public String select() {
        return "数据函数倍调用";
    }
}

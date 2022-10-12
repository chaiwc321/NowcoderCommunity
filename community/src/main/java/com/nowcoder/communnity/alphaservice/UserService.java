package com.nowcoder.communnity.alphaservice;

import com.nowcoder.communnity.alphadata.UserMapper;
import com.nowcoder.communnity.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    UserMapper userMapper;
    public User findUser(int Id){
        return userMapper.selectById(Id);
    }

}

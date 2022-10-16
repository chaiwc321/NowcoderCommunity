package com.nowcoder.communnity.util;



import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class CommunityUtil {

    public static String generateUUID(){
        return UUID.randomUUID().toString().replaceAll("-",""); //UUID包生成随机码，转字符串格式，把里面生成的-去掉
    }

    // 密码加密
    public static String md5(String key){
        if (StringUtils.isBlank(key)){
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }
}

package com.nowcoder.communnity.util;



import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class CommunityUtil {

    // 生成激活码
    public static String generateUUID(){
        return UUID.randomUUID().toString().replaceAll("-","");  // UUID包生成随机码，转字符串格式，把里面生成的-去掉
    }

    // 密码加密
    public static String md5(String key){
        if (StringUtils.isBlank(key)){
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }

    public static String getJSONString(int code, String msg, Map<String, Object> map) {
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("msg", msg);
        if(map != null) {
            for (String key:map.keySet()
                 ) {
                json.put(key, map.get(key));
            }
        }
        return json.toJSONString();
    }

    public static String getJSONString (int code, String msg) {
        return getJSONString(code, msg, null);
    }

    public static String getJSONString (int code) {
        return getJSONString(code, null, null);
    }

}

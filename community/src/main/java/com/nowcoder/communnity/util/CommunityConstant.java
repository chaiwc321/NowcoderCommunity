package com.nowcoder.communnity.util;

public interface CommunityConstant {
    int ACTIVATION_SUCCESS = 0;
    int ACTIVATION_REPEAT = 1;
    int ACTIVATION_FAILURE = 2;

    /**
     * 默认状态的登录凭证的超时时间
     */
    int DEFAULT_EXPIRED_SECONDS = 3600*12;

    /**
     * 记住状态的登录凭证超时时间
     */
    int REMEMBER_EXPIRED_SECONDS = 3600*24*100;

    int ENTITY_TYPE_POST = 1;

    int ENTITY_TYPE_COMMENT = 2;

    int ENTITY_TYPE_PERSON = 3;
}

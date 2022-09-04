package org.celery.command.controller

enum class ExecutionPermission {

        /**
         * 仅Bot的超级管理员可使用(超级管理员在任何时候都可使用任何命令)
         */
        SUPER_USER,

        /**
         * 仅群主可使用
         */
        OWNER,

        /**
         * 管理员和群主可使用
         */
        OPERATOR,

        /**
         * 普通群员可使用
         */
        NORMAL_MEMBER,

        /**
         * 群员(包括匿名)可使用
         */
        ANY_MEMBER,

        /**
         * 仅私聊可用
         */
        FRIEND_ONLY
    }
package com.example.campussecondhand.service;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 单元测试用的无资源事务模板。
 *
 * <p>被测对象全部是 Mockito mock，不存在真实数据库连接，因此这里只需
 * 原样执行回调、不做任何提交/回滚，即可验证业务分支而不依赖 Spring 上下文。</p>
 */
class ResourcelessTransactionTemplate extends TransactionTemplate {

    ResourcelessTransactionTemplate() {
        super(new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
                // 无资源可绑定
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
                // 无资源可提交
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
                // 无资源可回滚
            }
        });
    }
}

/*
 * Copyright © 2013-2021, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.redis.internal;

import com.google.inject.PrivateModule;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import org.seedstack.redis.RedisExceptionHandler;
import org.seedstack.seed.core.internal.transaction.TransactionalClassProxy;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;

import java.util.Map;

class RedisModule extends PrivateModule {
    private final Map<String, Class<? extends RedisExceptionHandler>> exceptionHandlerClasses;
    private final Map<String, JedisCluster> jedisClusters;
    private final Map<String, JedisPool> jediPools;

    public RedisModule(Map<String, JedisPool> jedisPools, Map<String, Class<? extends RedisExceptionHandler>> exceptionHandlerClasses, Map<String, JedisCluster> jedisClusters) {
        this.jediPools = jedisPools;
        this.exceptionHandlerClasses = exceptionHandlerClasses;
        this.jedisClusters = jedisClusters;
    }

    @Override
    protected void configure() {
        RedisLink<Transaction> transactionRedisLink = new RedisLink<>();
        bind(Transaction.class).toInstance(TransactionalClassProxy.create(Transaction.class, transactionRedisLink));

        RedisLink<Pipeline> pipelineRedisLink = new RedisLink<>();
        bind(Pipeline.class).toInstance(TransactionalClassProxy.create(Pipeline.class, pipelineRedisLink));

        jediPools.forEach((key, value) -> {
            bindClient(key, value, transactionRedisLink, pipelineRedisLink);
            bind(JedisPool.class).annotatedWith(Names.named(key)).toInstance(value);
            expose(JedisPool.class).annotatedWith(Names.named(key));
        });

        expose(Transaction.class);
        expose(Pipeline.class);

        jedisClusters.forEach((key, value) -> {
            bind(JedisCluster.class).annotatedWith(Names.named(key)).toInstance(value);
            expose(JedisCluster.class).annotatedWith(Names.named(key));
        });
    }

    private void bindClient(String name, JedisPool jedisPool, RedisLink<Transaction> transactionRedisLink, RedisLink<Pipeline> pipelineRedisLink) {
        Class<? extends RedisExceptionHandler> exceptionHandlerClass = exceptionHandlerClasses.get(name);

        if (exceptionHandlerClass != null) {
            bind(RedisExceptionHandler.class).annotatedWith(Names.named(name)).to(exceptionHandlerClass);
        } else {
            bind(RedisExceptionHandler.class).annotatedWith(Names.named(name)).toProvider(Providers.<RedisExceptionHandler>of(null));
        }

        RedisTransactionHandler redisTransactionHandler = new RedisTransactionHandler(transactionRedisLink, jedisPool);
        bind(RedisTransactionHandler.class).annotatedWith(Names.named(name)).toInstance(redisTransactionHandler);

        RedisPipelinedTransactionHandler redisPipelinedTransactionHandler = new RedisPipelinedTransactionHandler(pipelineRedisLink, jedisPool);
        bind(RedisPipelinedTransactionHandler.class).annotatedWith(Names.named(name)).toInstance(redisPipelinedTransactionHandler);

        expose(RedisExceptionHandler.class).annotatedWith(Names.named(name));
        expose(RedisTransactionHandler.class).annotatedWith(Names.named(name));
        expose(RedisPipelinedTransactionHandler.class).annotatedWith(Names.named(name));
    }
}

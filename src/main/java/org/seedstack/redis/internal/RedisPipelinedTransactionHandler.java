/*
 * Copyright © 2013-2021, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.redis.internal;

import org.seedstack.seed.transaction.spi.TransactionMetadata;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;


class RedisPipelinedTransactionHandler implements org.seedstack.seed.transaction.spi.TransactionHandler<Pipeline> {
    private final RedisLink<Pipeline> redisLink;
    private final JedisPool jedisPool;

    RedisPipelinedTransactionHandler(RedisLink<Pipeline> redisLink, JedisPool jedisPool) {
        this.redisLink = redisLink;
        this.jedisPool = jedisPool;
    }

    @Override
    public void doInitialize(TransactionMetadata transactionMetadata) {
        this.redisLink.push(this.jedisPool.getResource());
    }

    @Override
    public Pipeline doCreateTransaction() {
        RedisLink<Pipeline>.Holder holder = this.redisLink.getHolder();
        Pipeline pipeline = holder.getJedis().pipelined();
        pipeline.multi();
        holder.setTransaction(pipeline);
        return pipeline;
    }

    @Override
    public void doJoinGlobalTransaction() {
        // not supported
    }

    @Override
    public void doBeginTransaction(Pipeline currentTransaction) {
        // nothing to do (transaction already began)
    }

    @Override
    public void doCommitTransaction(Pipeline currentTransaction) {
        currentTransaction.exec();
    }

    @Override
    public void doMarkTransactionAsRollbackOnly(Pipeline currentTransaction) {
        // not supported
    }

    @Override
    public void doRollbackTransaction(Pipeline currentTransaction) {
        currentTransaction.clear();
    }

    @Override
    public void doReleaseTransaction(Pipeline currentTransaction) {
        currentTransaction.close();
    }

    @Override
    public void doCleanup() {
        this.redisLink.pop().close();
    }

    @Override
    public Pipeline getCurrentTransaction() {
        RedisLink<Pipeline>.Holder holder = this.redisLink.getHolder();

        if (holder == null) {
            return null;
        } else {
            return holder.getTransaction();
        }
    }
}

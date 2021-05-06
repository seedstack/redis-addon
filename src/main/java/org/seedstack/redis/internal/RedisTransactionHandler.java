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
import redis.clients.jedis.Transaction;


class RedisTransactionHandler implements org.seedstack.seed.transaction.spi.TransactionHandler<Transaction> {
    private final RedisLink<Transaction> redisLink;
    private final JedisPool jedisPool;

    RedisTransactionHandler(RedisLink<Transaction> redisLink, JedisPool jedisPool) {
        this.redisLink = redisLink;
        this.jedisPool = jedisPool;
    }

    @Override
    public void doInitialize(TransactionMetadata transactionMetadata) {
        this.redisLink.push(this.jedisPool.getResource());
    }

    @Override
    public Transaction doCreateTransaction() {
        RedisLink<Transaction>.Holder holder = this.redisLink.getHolder();
        Transaction transaction = holder.getJedis().multi();
        holder.setTransaction(transaction);
        return transaction;
    }

    @Override
    public void doJoinGlobalTransaction() {
        // not supported
    }

    @Override
    public void doBeginTransaction(Transaction currentTransaction) {
        // nothing to do (transaction already began)
    }

    @Override
    public void doCommitTransaction(Transaction currentTransaction) {
        currentTransaction.exec();
    }

    @Override
    public void doMarkTransactionAsRollbackOnly(Transaction currentTransaction) {
        // not supported
    }

    @Override
    public void doRollbackTransaction(Transaction currentTransaction) {
        currentTransaction.clear();
    }

    @Override
    public void doReleaseTransaction(Transaction currentTransaction) {
        currentTransaction.close();
    }

    @Override
    public void doCleanup() {
        this.redisLink.pop().close();
    }

    @Override
    public Transaction getCurrentTransaction() {
        RedisLink<Transaction>.Holder holder = this.redisLink.getHolder();

        if (holder == null) {
            return null;
        } else {
            return holder.getTransaction();
        }
    }
}

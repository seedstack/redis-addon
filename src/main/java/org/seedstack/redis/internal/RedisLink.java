/*
 * Copyright © 2013-2021, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.redis.internal;

import org.seedstack.seed.SeedException;
import org.seedstack.seed.transaction.spi.TransactionalLink;
import redis.clients.jedis.Jedis;

import java.util.ArrayDeque;
import java.util.Deque;

class RedisLink<T> implements TransactionalLink<T> {
    private final ThreadLocal<Deque<Holder>> perThreadObjectContainer = new ThreadLocal<Deque<Holder>>() {
        @Override
        protected Deque<Holder> initialValue() {
            return new ArrayDeque<Holder>();
        }
    };

    public T get() {
        Holder holder = perThreadObjectContainer.get().peek();

        if (holder == null || holder.transaction == null) {
            throw SeedException.createNew(RedisErrorCode.ACCESSING_REDIS_OUTSIDE_TRANSACTION);
        }

        return holder.transaction;
    }

    Holder getHolder() {
        return perThreadObjectContainer.get().peek();
    }

    void push(Jedis jedis) {
        perThreadObjectContainer.get().push(new Holder(jedis));
    }

    Jedis pop() {
        Deque<Holder> holders = perThreadObjectContainer.get();
        Holder holder = holders.pop();
        if (holders.isEmpty()) {
            perThreadObjectContainer.remove();
        }
        return holder.jedis;
    }

    class Holder {
        private final Jedis jedis;
        private T transaction;

        private Holder(Jedis jedis) {
            this.jedis = jedis;
        }

        Jedis getJedis() {
            return jedis;
        }

        T getTransaction() {
            return transaction;
        }

        void setTransaction(T transaction) {
            this.transaction = transaction;
        }
    }
}

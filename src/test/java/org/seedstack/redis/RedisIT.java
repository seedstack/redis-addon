/*
 * Copyright © 2013-2021, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.redis;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seedstack.seed.SeedException;
import org.seedstack.seed.testing.junit4.SeedITRunner;
import org.seedstack.seed.transaction.Transactional;
import redis.clients.jedis.*;

import javax.inject.Inject;
import javax.inject.Named;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SeedITRunner.class)
public class RedisIT {
    @Inject
    private Transaction transaction;
    @Inject
    private Pipeline pipeline;
    @Inject
    @Named("client1")
    private JedisPool jedisPool;
    @Inject
    @Named("cluster1")
    private JedisCluster jedisCluster;

    @Test
    public void redis_is_injectable() {
        assertThat(transaction).isNotNull();
        assertThat(jedisPool).isNotNull();
        assertThat(jedisCluster).isNotNull();
    }

    @Test(expected = SeedException.class)
    public void access_outside_transaction() {
        transaction.set("keyFail", "valueFail");
    }

    @Test
    public void simple_transaction() {
        addKey1();
        assertThat(retrieveKey1().get()).isEqualTo("value1value2");
    }

    @Test
    public void pipelined_transaction() {
        addKey2();
        assertThat(retrieveKey2().get()).isEqualTo("value3value4");
    }

    @Test
    public void plain_usage() {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.set("foo", "bar");
            jedis.zadd("sose", 0, "car");
            jedis.zadd("sose", 0, "bike");
            assertThat(jedis.get("foo")).isEqualTo("bar");
            assertThat(jedis.zrange("sose", 0, -1)).containsExactly("bike", "car");
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    @Transactional
    @Redis("client1")
    protected void addKey1() {
        transaction.set("key1", "value1");
        transaction.append("key1", "value2");
    }

    @Transactional
    @Redis(value = "client1", pipelined = true)
    protected void addKey2() {
        pipeline.set("key2", "value3");
        pipeline.append("key2", "value4");
    }

    @Transactional
    @Redis(value = "client1")
    protected Response<String> retrieveKey1() {
        return transaction.get("key1");
    }

    @Transactional
    @Redis(value = "client1", pipelined = true)
    protected Response<String> retrieveKey2() {
        return pipeline.get("key2");
    }
}

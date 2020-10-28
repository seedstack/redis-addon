/*
 * Copyright © 2013-2020, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.redis;

import org.seedstack.coffig.Config;
import org.seedstack.coffig.SingleValue;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Config("redis")
public class RedisConfig {
    private Map<String, ClientConfig> clients = new HashMap<>();
    private String defaultClient;

    public Map<String, ClientConfig> getClients() {
        return Collections.unmodifiableMap(clients);
    }

    public RedisConfig addClient(String name, ClientConfig clientConfig) {
        this.clients.put(name, clientConfig);
        return this;
    }

    public String getDefaultClient() {
        return defaultClient;
    }

    public RedisConfig setDefaultClient(String defaultClient) {
        this.defaultClient = defaultClient;
        return this;
    }

    public static class ClientConfig {
        @NotNull
        @SingleValue
        private URI uri;
        @NotNull
        private JedisPoolConfig poolConfig = new JedisPoolConfig();
        @Min(0)
        private int timeout = Protocol.DEFAULT_TIMEOUT;
        private Class<? extends RedisExceptionHandler> exceptionHandler;

        public URI getUri() {
            return uri;
        }

        public ClientConfig setUri(URI uri) {
            this.uri = uri;
            return this;
        }

        public JedisPoolConfig getPoolConfig() {
            return poolConfig;
        }

        public ClientConfig setPoolConfig(JedisPoolConfig poolConfig) {
            this.poolConfig = poolConfig;
            return this;
        }

        public int getTimeout() {
            return timeout;
        }

        public ClientConfig setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Class<? extends RedisExceptionHandler> getExceptionHandler() {
            return exceptionHandler;
        }

        public ClientConfig setExceptionHandler(Class<? extends RedisExceptionHandler> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }
    }
}

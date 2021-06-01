/*
 * Copyright © 2013-2021, The SeedStack authors <http://seedstack.org>
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
import java.util.*;

@Config("redis")
public class RedisConfig {
    private Map<String, ClientConfig> clients = new HashMap<>();
    private String defaultClient;
    private Map<String, ClusterConfig> clusters = new HashMap<>();

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

    public Map<String, ClusterConfig> getClusters() {
        return Collections.unmodifiableMap(clusters);
    }

    public void addCluster(String name, ClusterConfig clusterConfig) {
        this.clusters.put(name, clusterConfig);
    }

    public static abstract class CommonConfig {
        @NotNull
        private JedisPoolConfig poolConfig = new JedisPoolConfig();
        @Min(0)
        private int timeout = Protocol.DEFAULT_TIMEOUT;
        @Min(0)
        private int socketTimeout = Protocol.DEFAULT_TIMEOUT;
        @Min(0)
        private int socketInfiniteTimeout = Protocol.DEFAULT_TIMEOUT;
        private boolean ssl = true;

        public JedisPoolConfig getPoolConfig() {
            return poolConfig;
        }

        public CommonConfig setPoolConfig(JedisPoolConfig poolConfig) {
            this.poolConfig = poolConfig;
            return this;
        }

        public int getTimeout() {
            return timeout;
        }

        public CommonConfig setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public int getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
        }

        public int getSocketInfiniteTimeout() {
            return socketInfiniteTimeout;
        }

        public void setSocketInfiniteTimeout(int socketInfiniteTimeout) {
            this.socketInfiniteTimeout = socketInfiniteTimeout;
        }

        public boolean isSsl() {
            return ssl;
        }

        public CommonConfig setSsl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }
    }

    public static class ClientConfig extends CommonConfig {
        @NotNull
        @SingleValue
        private URI uri;
        private Class<? extends RedisExceptionHandler> exceptionHandler;

        public URI getUri() {
            return uri;
        }

        public ClientConfig setUri(URI uri) {
            this.uri = uri;
            return this;
        }

        public Class<? extends RedisExceptionHandler> getExceptionHandler() {
            return exceptionHandler;
        }

        public CommonConfig setExceptionHandler(Class<? extends RedisExceptionHandler> exceptionHandler) {
            this.exceptionHandler = exceptionHandler;
            return this;
        }
    }

    public static class ClusterConfig extends CommonConfig {
        public static final int DEFAULT_MAX_ATTEMPTS = 5;
        @SingleValue
        private Set<String> hostAndPorts = new HashSet<>();
        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private String user;
        private String password;
        private String clientName;

        public Set<String> getHostAndPorts() {
            return hostAndPorts;
        }

        public ClusterConfig setHostAndPorts(Set<String> hostAndPorts) {
            this.hostAndPorts = hostAndPorts;
            return this;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public ClusterConfig setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public String getUser() {
            return user;
        }

        public ClusterConfig setUser(String user) {
            this.user = user;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public ClusterConfig setPassword(String password) {
            this.password = password;
            return this;
        }

        public String getClientName() {
            return clientName;
        }

        public ClusterConfig setClientName(String clientName) {
            this.clientName = clientName;
            return this;
        }
    }
}

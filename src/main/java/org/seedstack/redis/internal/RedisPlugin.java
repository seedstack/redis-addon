/*
 * Copyright © 2013-2021, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.redis.internal;

import com.google.common.collect.Lists;
import io.nuun.kernel.api.plugin.InitState;
import io.nuun.kernel.api.plugin.context.InitContext;
import io.nuun.kernel.api.plugin.request.ClasspathScanRequest;
import org.seedstack.redis.RedisConfig;
import org.seedstack.redis.RedisExceptionHandler;
import org.seedstack.seed.SeedException;
import org.seedstack.seed.core.internal.AbstractSeedPlugin;
import org.seedstack.seed.crypto.spi.SSLProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RedisPlugin extends AbstractSeedPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPlugin.class);
    private final Map<String, JedisPool> jedisPools = new HashMap<>();
    private final Map<String, JedisCluster> jedisClusters = new HashMap<>();
    private final Map<String, Class<? extends RedisExceptionHandler>> exceptionHandlerClasses = new HashMap<String, Class<? extends RedisExceptionHandler>>();

    @Override
    public String name() {
        return "redis";
    }

    @Override
    protected Collection<Class<?>> dependencies() {
        return Lists.newArrayList(SSLProvider.class);
    }

    @Override
    public Collection<ClasspathScanRequest> classpathScanRequests() {
        return classpathScanRequestBuilder()
                .subtypeOf(RedisExceptionHandler.class)
                .build();
    }

    @Override
    public InitState initialize(InitContext initContext) {
        RedisConfig redisConfig = getConfiguration(RedisConfig.class);

        if (redisConfig.getClients().isEmpty() && redisConfig.getClusters().isEmpty()) {
            LOGGER.info("No Redis client or cluster configured, Redis support disabled");
            return InitState.INITIALIZED;
        }

        for (Map.Entry<String, RedisConfig.ClientConfig> clientEntry : redisConfig.getClients().entrySet()) {
            String clientName = clientEntry.getKey();
            RedisConfig.ClientConfig clientConfig = clientEntry.getValue();

            Class<? extends RedisExceptionHandler> exceptionHandlerClass = clientConfig.getExceptionHandler();
            if (exceptionHandlerClass != null) {
                exceptionHandlerClasses.put(clientName, exceptionHandlerClass);
            }

            try {
                LOGGER.info("Creating Jedis Pool for client {}", clientName);
                jedisPools.put(clientName, createJedisPool(clientConfig, initContext.dependency(SSLProvider.class)));
            } catch (Exception e) {
                throw SeedException.wrap(e, RedisErrorCode.UNABLE_TO_CREATE_CLIENT).put("clientName", clientName);
            }
        }

        for (Map.Entry<String, RedisConfig.ClusterConfig> clusterEntry : redisConfig.getClusters().entrySet()) {
            String clusterName = clusterEntry.getKey();
            try {
                LOGGER.info("Creating Jedis Cluster {}", clusterName);
                JedisCluster jedisCluster = createJedisCluster(clusterEntry.getValue(), initContext.dependency(SSLProvider.class));
                if (jedisCluster.getClusterNodes().isEmpty()) {
                    LOGGER.warn("Unable to connect to any node of the cluster {} at startup", clusterName);
                }
                jedisClusters.put(clusterName, jedisCluster);
            } catch (Exception e) {
                throw SeedException.wrap(e, RedisErrorCode.UNABLE_TO_CREATE_CLIENT).put("clusterName", clusterName);
            }
        }

        return InitState.INITIALIZED;
    }

    @Override
    public Object nativeUnitModule() {
        return new RedisModule(jedisPools, exceptionHandlerClasses, jedisClusters);
    }

    @Override
    public void stop() {
        jedisPools.forEach((key, value) -> {
            LOGGER.info("Shutting down {} Jedis pool", key);
            try {
                value.close();
            } catch (Exception e) {
                LOGGER.error(String.format("Unable to properly close %s Jedis pool", key), e);
            }
        });

        jedisClusters.forEach((key, value) -> {
            LOGGER.info("Shutting down {} Jedis cluster", key);
            try {
                value.close();
            } catch (Exception e) {
                LOGGER.error(String.format("Unable to properly close %s Jedis cluster", key), e);
            }
        });
    }


    private JedisPool createJedisPool(RedisConfig.ClientConfig clientConfig, SSLProvider sslProvider) {
        if (sslProvider.sslContext().isPresent()) {
            SSLContext sslContext = sslProvider.sslContext().get();
            return new JedisPool(
                    clientConfig.getPoolConfig(),
                    clientConfig.getUri(),
                    clientConfig.getTimeout(),
                    clientConfig.getSocketTimeout(),
                    clientConfig.getSocketInfiniteTimeout(),
                    sslContext.getSocketFactory(),
                    sslContext.getSupportedSSLParameters(),
                    null // default
            );
        } else {
            return new JedisPool(
                    clientConfig.getPoolConfig(),
                    clientConfig.getUri(),
                    clientConfig.getTimeout(),
                    clientConfig.getSocketTimeout()
            );
        }
    }

    private JedisCluster createJedisCluster(RedisConfig.ClusterConfig clusterConfig, SSLProvider sslProvider) {
        SSLSocketFactory sslSocketFactory;
        SSLParameters sslParameters;
        boolean ssl;
        if (sslProvider.sslContext().isPresent()) {
            SSLContext sslContext = sslProvider.sslContext().get();
            sslSocketFactory = sslContext.getSocketFactory();
            sslParameters = sslContext.getSupportedSSLParameters();
            ssl = true;
        } else {
            sslSocketFactory = null;
            sslParameters = null;
            ssl = false;
        }

        return new JedisCluster(
                clusterConfig.getHostAndPorts().stream().map(HostAndPort::from).collect(Collectors.toSet()),
                clusterConfig.getTimeout(),
                clusterConfig.getSocketTimeout(),
                clusterConfig.getSocketInfiniteTimeout(),
                clusterConfig.getMaxAttempts(),
                clusterConfig.getUser(),
                clusterConfig.getPassword(),
                clusterConfig.getClientName(),
                clusterConfig.getPoolConfig(),
                ssl,
                sslSocketFactory,
                sslParameters,
                null, // default
                null // no mapping
        );
    }
}

/*
 * Copyright © 2013-2021, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.redis.internal;

import com.google.common.base.Strings;
import org.aopalliance.intercept.MethodInvocation;
import org.seedstack.redis.Redis;
import org.seedstack.redis.RedisConfig;
import org.seedstack.redis.RedisExceptionHandler;
import org.seedstack.seed.Application;
import org.seedstack.seed.SeedException;
import org.seedstack.seed.transaction.spi.TransactionMetadata;
import org.seedstack.seed.transaction.spi.TransactionMetadataResolver;

import javax.inject.Inject;
import java.util.Optional;

/**
 * This {@link TransactionMetadataResolver} resolves metadata for transactions marked
 * with {@link Redis}.
 */
class RedisTransactionMetadataResolver implements TransactionMetadataResolver {
    @Inject
    private Application application;

    @Override
    public TransactionMetadata resolve(MethodInvocation methodInvocation, TransactionMetadata defaults) {
        Optional<Redis> redisOptional = RedisResolver.INSTANCE.apply(methodInvocation.getMethod());

        if (redisOptional.isPresent() || RedisTransactionHandler.class.equals(defaults.getHandler()) || RedisPipelinedTransactionHandler.class.equals(defaults.getHandler())) {
            TransactionMetadata result = new TransactionMetadata();

            result.setExceptionHandler(RedisExceptionHandler.class);
            if (redisOptional.isPresent()) {
                result.setResource(redisOptional.get().value());
            } else {
                String defaultClient = application.getConfiguration().get(RedisConfig.class).getDefaultClient();
                if (!Strings.isNullOrEmpty(defaultClient)) {
                    result.setResource(defaultClient);
                } else {
                    throw SeedException.createNew(RedisErrorCode.NO_REDIS_CLIENT_SPECIFIED_FOR_TRANSACTION)
                            .put("method", methodInvocation.getMethod().toString());
                }
            }

            if (redisOptional.isPresent()) {
                result.setHandler(redisOptional.get().pipelined() ? RedisPipelinedTransactionHandler.class : RedisTransactionHandler.class);
            } else if (RedisTransactionHandler.class.equals(defaults.getHandler())) {
                result.setHandler(RedisTransactionHandler.class);
            } else if (RedisPipelinedTransactionHandler.class.equals(defaults.getHandler())) {
                result.setHandler(RedisPipelinedTransactionHandler.class);
            }

            return result;
        }

        return null;
    }
}
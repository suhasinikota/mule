/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.runtime.ConfigurationProvider;
import org.mule.extension.api.runtime.ConfigurationStats;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.extension.api.runtime.ExpirableConfigurationProvider;
import org.mule.extension.api.runtime.ExpirationPolicy;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.util.collection.ImmutableListCollector;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A {@link ConfigurationProvider} which continuously evaluates the same
 * {@link ResolverSet} and then uses the resulting {@link ResolverSetResult}
 * to build an instance of type {@code T}
 * <p/>
 * Although each invocation to {@link #get(Object)} is guaranteed to end up in an invocation
 * to {@link #resolverSet#resolve(MuleEvent)}, the resulting {@link ResolverSetResult} might not end up
 * generating a new instance. This is so because {@link ResolverSetResult} instances are put in a cache to
 * guarantee that equivalent evaluations of the {@code resolverSet} return the same instance.
 *
 * @param <T> the generic type of the provided {@link ConfigurationInstance}
 * @since 4.0.0
 */
public final class DynamicConfigurationProvider<T> extends LifecycleAwareConfigurationProvider<T> implements ExpirableConfigurationProvider<T>
{

    private final ConfigurationInstanceFactory<T> configurationInstanceFactory;
    private final ResolverSet resolverSet;
    private final ExpirationPolicy expirationPolicy;

    private final Map<ResolverSetResult, ConfigurationInstance<T>> cache = new ConcurrentHashMap<>();
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private final Lock cacheReadLock = cacheLock.readLock();
    private final Lock cacheWriteLock = cacheLock.writeLock();

    /**
     * Creates a new instance
     *
     * @param name               this provider's name
     * @param configurationModel the model for the returned configurations
     * @param resolverSet        the {@link ResolverSet} that provides the configuration's parameter values
     * @param expirationPolicy   the {@link ExpirationPolicy} for the unused instances
     */
    public DynamicConfigurationProvider(String name,
                                        ConfigurationModel configurationModel,
                                        ResolverSet resolverSet,
                                        ExpirationPolicy expirationPolicy)
    {
        super(name, configurationModel);
        configurationInstanceFactory = new ConfigurationInstanceFactory<>(configurationModel, resolverSet);
        this.resolverSet = resolverSet;
        this.expirationPolicy = expirationPolicy;
    }

    /**
     * Evaluates {@link #resolverSet} using the given {@code muleEvent} and returns
     * an instance produced with the result. For equivalent {@link ResolverSetResult}s
     * it will return the same instance.
     *
     * @param muleEvent the current {@link MuleEvent}
     * @return the resolved {@link ConfigurationInstance}
     */
    @Override
    public ConfigurationInstance<T> get(Object muleEvent)
    {
        try
        {
            ResolverSetResult result = resolverSet.resolve((MuleEvent) muleEvent);
            return getConfiguration(result);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    private ConfigurationInstance<T> getConfiguration(ResolverSetResult resolverSetResult) throws Exception
    {
        ConfigurationInstance<T> configuration;
        cacheReadLock.lock();
        try
        {
            configuration = cache.get(resolverSetResult);
            if (configuration != null)
            {
                //important to account between the boundaries of the lock to prevent race condition
                updateUsageStatistic(configuration);
                return configuration;
            }
        }
        finally
        {
            cacheReadLock.unlock();
        }

        cacheWriteLock.lock();
        try
        {
            // re-check in case some other thread beat us to it...
            configuration = cache.get(resolverSetResult);
            if (configuration == null)
            {
                configuration = createConfiguration(resolverSetResult);
                cache.put(resolverSetResult, configuration);
            }

            // accounting here for the same reasons as above
            updateUsageStatistic(configuration);
            return configuration;
        }
        finally
        {
            cacheWriteLock.unlock();
        }
    }

    private void updateUsageStatistic(ConfigurationInstance<T> configuration)
    {
        MutableConfigurationStats stats = (MutableConfigurationStats) configuration.getStatistics();
        stats.updateLastUsed();
    }

    private ConfigurationInstance<T> createConfiguration(ResolverSetResult result) throws MuleException
    {
        ConfigurationInstance<T> configuration = configurationInstanceFactory.createConfiguration(getName(), result);
        registerConfiguration(configuration);

        return configuration;
    }

    @Override
    protected void registerConfiguration(ConfigurationInstance<T> configuration)
    {
        try
        {
            if (lifecycleManager.isPhaseComplete(Initialisable.PHASE_NAME))
            {
                initialiseConfig(configuration);
            }

            if (lifecycleManager.isPhaseComplete(Startable.PHASE_NAME))
            {
                startConfig(configuration);
            }
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(createStaticMessage("Could not register configuration of key " + getName()), e);
        }

        super.registerConfiguration(configuration);
    }

    @Override
    public List<ConfigurationInstance<T>> getExpired()
    {
        cacheWriteLock.lock();
        try
        {
            return cache.entrySet()
                    .stream()
                    .filter(entry -> isExpired(entry.getValue()))
                    .map(entry -> {
                        cache.remove(entry.getKey());
                        return entry.getValue();
                    }).collect(new ImmutableListCollector<>());
        }
        finally
        {
            cacheWriteLock.unlock();
        }
    }

    private boolean isExpired(ConfigurationInstance<T> configuration)
    {
        ConfigurationStats stats = configuration.getStatistics();
        return stats.getInflightOperations() == 0 &&
               expirationPolicy.isExpired(stats.getLastUsedMillis(), TimeUnit.MILLISECONDS);
    }
}

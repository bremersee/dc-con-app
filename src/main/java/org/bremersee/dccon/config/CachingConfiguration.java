/*
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bremersee.dccon.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * The caching configuration.
 *
 * @author Christian Bremer
 */
@Configuration
//@EnableCaching
@Slf4j
public class CachingConfiguration {

  /*
  import java.io.Serializable;
  import java.util.Map;
  import java.util.concurrent.TimeUnit;
  import javax.cache.configuration.FactoryBuilder;
  import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
  import javax.cache.configuration.MutableConfiguration;
  import javax.cache.event.CacheEntryCreatedListener;
  import javax.cache.event.CacheEntryEvent;
  import javax.cache.event.CacheEntryEventFilter;
  import javax.cache.event.CacheEntryExpiredListener;
  import javax.cache.event.CacheEntryListener;
  import javax.cache.event.CacheEntryListenerException;
  import javax.cache.event.CacheEntryRemovedListener;
  import javax.cache.event.CacheEntryUpdatedListener;
  import javax.cache.expiry.CreatedExpiryPolicy;
  import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
  import org.springframework.context.annotation.Bean;

  @Bean
  public JCacheManagerCustomizer cacheManagerCustomizer() {
    return cacheManager -> {
      final TimeUnit durationUnit = TimeUnit.SECONDS;
      if (cacheManager.getCache("dhcp-leases-by-ip") == null) {
        log.info("msg=[Creating cache 'dhcp-leases-by-ip']");
        cacheManager.createCache(
            "dhcp-leases-by-ip",
            new MutableConfiguration<Object, Map<String, DhcpLease>>()
                .setExpiryPolicyFactory(CreatedExpiryPolicy
                    .factoryOf(new javax.cache.expiry.Duration(durationUnit, 30L)))
                .setStoreByValue(false)
                .setStatisticsEnabled(true)
                .addCacheEntryListenerConfiguration(cacheEntryListenerConfiguration())
        );
      }
      if (cacheManager.getCache("dhcp-leases-by-name") == null) {
        log.info("msg=[Creating cache 'dhcp-leases-by-name']");
        cacheManager.createCache(
            "dhcp-leases-by-name",
            new MutableConfiguration<Object, Map<String, DhcpLease>>()
                .setExpiryPolicyFactory(CreatedExpiryPolicy
                    .factoryOf(new javax.cache.expiry.Duration(durationUnit, 30L)))
                .setStoreByValue(false)
                .setStatisticsEnabled(true)
                .addCacheEntryListenerConfiguration(cacheEntryListenerConfiguration())
        );
      }
    };
  }

  @Bean
  public MutableCacheEntryListenerConfiguration<Object, Map<String, DhcpLease>> cacheEntryListenerConfiguration() {
    return new MutableCacheEntryListenerConfiguration<>(
        FactoryBuilder.factoryOf(new DhcpLeaseCacheEntryListener()),
        FactoryBuilder.factoryOf(new DhcpLeaseCacheEntryEventFilter()),
        false, true);
  }

  @Slf4j
  static class DhcpLeaseCacheEntryEventFilter
      implements Serializable, CacheEntryEventFilter<Object, Map<String, DhcpLease>> {

    @Override
    public boolean evaluate(CacheEntryEvent<?, ? extends Map<String, DhcpLease>> cacheEntryEvent)
        throws CacheEntryListenerException {
      log.info("msg=[Evaluate cache entry event] eventType=[{}]", cacheEntryEvent.getEventType());
      return true;
    }
  }

  @Slf4j
  static class DhcpLeaseCacheEntryListener implements
      Serializable,
      CacheEntryListener<Object, Map<String, DhcpLease>>,
      CacheEntryCreatedListener<Object, Map<String, DhcpLease>>,
      CacheEntryUpdatedListener<Object, Map<String, DhcpLease>>,
      CacheEntryRemovedListener<Object, Map<String, DhcpLease>>,
      CacheEntryExpiredListener<Object, Map<String, DhcpLease>> {

    @Override
    public void onCreated(Iterable<CacheEntryEvent<?, ? extends Map<String, DhcpLease>>> iterable)
        throws CacheEntryListenerException {
      iterable.iterator().forEachRemaining(cacheEntryEvent -> log
          .info("msg=[Dhcp lease cache created.] values=[{}[", cacheEntryEvent.getValue()));
    }

    @Override
    public void onUpdated(Iterable<CacheEntryEvent<?, ? extends Map<String, DhcpLease>>> iterable)
        throws CacheEntryListenerException {
      iterable.iterator().forEachRemaining(cacheEntryEvent -> log
          .info("msg=[Dhcp lease cache updated.] values=[{}[", cacheEntryEvent.getValue()));
    }

    @Override
    public void onRemoved(Iterable<CacheEntryEvent<?, ? extends Map<String, DhcpLease>>> iterable)
        throws CacheEntryListenerException {
      iterable.iterator().forEachRemaining(cacheEntryEvent -> log
          .info("msg=[Dhcp lease cache removed.] values=[{}[", cacheEntryEvent.getValue()));
    }

    @Override
    public void onExpired(Iterable<CacheEntryEvent<?, ? extends Map<String, DhcpLease>>> iterable)
        throws CacheEntryListenerException {
      iterable.iterator().forEachRemaining(cacheEntryEvent -> log
          .info("msg=[Dhcp lease cache expired.] values=[{}[", cacheEntryEvent.getValue()));
    }

  }
  */

}

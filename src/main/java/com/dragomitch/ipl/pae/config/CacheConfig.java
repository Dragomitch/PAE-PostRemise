package com.dragomitch.ipl.pae.config;

import com.dragomitch.ipl.pae.application.ReferenceDataCaches;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * The cache of the reference data: Spring's cache abstraction on the {@code @Cacheable} use cases
 * of {@code application}, backed by Caffeine (in-process, bounded, entries expiring after a time to
 * live set by {@code spring.cache.caffeine.spec} in {@code application.properties}).
 *
 * <p>The caches are the fixed set of {@link #CACHE_NAMES}: a use case naming another cache fails
 * instead of creating one silently. They are listed, and can be emptied, on {@code
 * /actuator/caches} (signed in, see {@link SecurityConfig}).
 *
 * <p>The caching advice runs outside the transaction advice ({@code order} just above the {@code
 * LOWEST_PRECEDENCE} of the transactions): a cache hit opens no transaction and takes no
 * connection, and the eviction of {@code @CacheEvict} happens once the transaction of the change is
 * committed, never before (a reader would otherwise put the data from before the change back).
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 1)
public class CacheConfig {

  /** The caches of the reference data, named by the use cases that fill them. */
  static final List<String> CACHE_NAMES =
      List.of(
          ReferenceDataCaches.COUNTRIES,
          ReferenceDataCaches.OPTIONS,
          ReferenceDataCaches.PROGRAMMES,
          ReferenceDataCaches.DOCUMENTS,
          ReferenceDataCaches.DENIAL_REASONS);

  /**
   * The Caffeine cache manager of the {@link #CACHE_NAMES}.
   *
   * @param specification the Caffeine specification of every cache ({@code
   *     spring.cache.caffeine.spec}: size bound and time to live)
   * @return the cache manager
   */
  @Bean
  public CaffeineCacheManager cacheManager(
      @Value("${spring.cache.caffeine.spec}") String specification) {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCacheSpecification(specification);
    // the use cases return lists or throw: a null would be a bug, not a value to remember
    cacheManager.setAllowNullValues(false);
    cacheManager.setCacheNames(CACHE_NAMES);
    return cacheManager;
  }
}

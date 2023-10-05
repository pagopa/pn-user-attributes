package it.pagopa.pn.user.attributes.springbootcfg;

import it.pagopa.pn.commons.configs.cache.CacheConfigs;
import it.pagopa.pn.commons.configs.cache.PnCacheConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({ PnCacheConfiguration.class})
public class CacheConfigsActivation extends CacheConfigs {
}

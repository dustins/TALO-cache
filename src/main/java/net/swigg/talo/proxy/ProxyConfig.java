/*
 * Copyright
 */

package net.swigg.talo.proxy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Class Description
 *
 * @author Dustin Sweigart <dustin@swigg.net>
 */
@Configuration
public class ProxyConfig {
    @Bean
    public ProxyController proxyController() {
        return new ProxyController();
    }
}

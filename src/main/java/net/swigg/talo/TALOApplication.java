/*
 * Copyright
 */

package net.swigg.talo;

import net.swigg.talo.admin.AdminConfig;
import net.swigg.talo.proxy.ProxyConfig;
import net.swigg.talo.proxy.ProxyConfiguration;
import net.swigg.talo.proxy.ProxyController;
import net.swigg.talo.proxy.TALOServer;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Class Description
 *
 * @author Dustin Sweigart <dustin@swigg.net>
 */
@EnableAutoConfiguration
@Import({AdminConfig.class, ProxyConfig.class})
public class TALOApplication {
    static private final Logger LOGGER = LoggerFactory.getLogger(TALOApplication.class);

    static private final TALOConfiguration configuration = new TALOConfiguration();

    @Bean
    public TALOConfiguration taloConfiguration() {
        return configuration;
    }

    public static void main(String[] args) throws InterruptedException {
        CmdLineParser parser = new CmdLineParser(configuration);
        parser.getProperties().withUsageWidth(80);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            LOGGER.error("Unable to parse options.", e);
        }

        final ConfigurableApplicationContext applicationContext = SpringApplication.run(TALOApplication.class, args);

        final ProxyController proxyController = applicationContext.getBean(ProxyController.class);
        TALOServer server = proxyController.newServer(new ProxyConfiguration(configuration));
        server.run();
    }
}

/*
 * Copyright
 */

package net.swigg.talo.proxy;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Class Description
 *
 * @author Dustin Sweigart <dustin@swigg.net>
 */
public class ProxyController {
    private final List<TALOServer> proxyInstances = Lists.newArrayList();

    public TALOServer newServer(ProxyConfiguration configuration) {
        HttpChannelInitializer channelInitializer = new HttpChannelInitializer(new CacheHandler(configuration));

        TALOServer newServer = new TALOServer(configuration, channelInitializer);
        proxyInstances.add(newServer);

        return newServer;
    }
}

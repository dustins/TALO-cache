/*
 * Copyright
 */

package net.swigg.talo.proxy;

import net.swigg.talo.TALOConfiguration;

/**
 * Class Description
 *
 * @author Dustin Sweigart <dustin@swigg.net>
 */
public class ProxyConfiguration {
    private final String bindHost;

    private final int bindPort;

    private final String proxyHost;

    private final int proxyPort;

    public ProxyConfiguration(TALOConfiguration config) {
        this(config.getBindHost(), config.getBindPort(), config.getProxyHost(), config.getProxyPort());
    }

    public ProxyConfiguration(String proxyHost) {
        this("localhost", proxyHost);
    }

    public ProxyConfiguration(String bindHost, String proxyHost) {
        this(bindHost, 80, proxyHost, 80);
    }

    public ProxyConfiguration(String bindHost, int bindPort, String proxyHost, int proxyPort) {
        this.bindHost = bindHost;
        this.bindPort = bindPort;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
    }

    public String getBindHost() {
        return bindHost;
    }

    public int getBindPort() {
        return bindPort;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }
}

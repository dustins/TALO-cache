/*
 * Copyright
 */

package net.swigg.talo;

import org.kohsuke.args4j.Option;

/**
 * Class Description
 *
 * @author Dustin Sweigart <dustin@swigg.net>
 */
public class TALOConfiguration {
    @Option(name = "--bindHost", aliases = {"-a"}, usage = "local bind address")
    private String bindHost = "localhost";

    @Option(name = "--bindPort", aliases = {"-b"}, usage = "local bind port")
    private int bindPort = 80;

    @Option(name = "--proxyHost", aliases = {"-h"}, usage = "remote host address", required = true)
    private String proxyHost;

    @Option(name = "--proxyPort", aliases = {"-p"}, usage = "local bind port")
    private int proxyPort = 80;

    private Boolean managed = true;

    public String getBindHost() {
        return bindHost;
    }

    public void setBindHost(String bindHost) {
        this.bindHost = bindHost;
    }

    public int getBindPort() {
        return bindPort;
    }

    public void setBindPort(int bindPort) {
        this.bindPort = bindPort;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public Boolean isManaged() {
        return managed;
    }
}

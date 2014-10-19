/*
 * Copyright
 */

package net.swigg.talo.proxy;

import io.netty.handler.codec.http.FullHttpResponse;
import org.joda.time.DateTime;

/**
 * A wrapper for {@link FullHttpResponse} that ensures only a copy is ever sent to the
 * client.
 *
 * @author Dustin Sweigart <dustin@swigg.net>
 */
class ResponseHolder {
    /**
     * Unique identifier for each request.
     */
    private final Integer requestIdentifier;

    /**
     * The response to be sent to the client.
     */
    private final FullHttpResponse response;

    /**
     * Milliseconds from 1970-01-01T00:00:00Z when the response was created.
     */
    private final Long expiration;

    /**
     * Milliseconds from 1970-01-01T00:00:00Z when a cached response can be served regardless of its expiration.
     */
    private final Long grace;

    public ResponseHolder(Integer requestIdentifier, FullHttpResponse response, DateTime expiration) {
        this(requestIdentifier, response, expiration.getMillis(), expiration.getMillis());
    }

    public ResponseHolder(Integer requestIdentifier, FullHttpResponse response, DateTime expiration, DateTime grace) {
        this(requestIdentifier, response, expiration.getMillis(), grace.getMillis());
    }

    /**
     * Designated constructor.
     *
     * @param requestIdentifier
     * @param response
     * @param expiration
     * @param grace
     */
    public ResponseHolder(Integer requestIdentifier, FullHttpResponse response, Long expiration, Long grace) {
        this.requestIdentifier = requestIdentifier;
        this.grace = grace;
        this.expiration = expiration;
        this.response = response;
    }

    public Integer getRequestIdentifier() {
        return requestIdentifier;
    }

    public Long getGrace() {
        return grace;
    }

    public Long getExpiration() {
        return expiration;
    }

    public FullHttpResponse getResponse() {
        return response.copy();
    }
}

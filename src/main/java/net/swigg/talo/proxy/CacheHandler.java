/*
 * Copyright
 */

package net.swigg.talo.proxy;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AttributeKey;
import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Class Description
 *
 * @author Dustin Sweigart <dustin@swigg.net>
 */
@ChannelHandler.Sharable
class CacheHandler extends ChannelHandlerAdapter {
    static private final Logger LOGGER = LoggerFactory.getLogger(CacheHandler.class);

    private final ProxyConfiguration configuration;

    public CacheHandler(ProxyConfiguration proxyConfiguration) {
        this.configuration = proxyConfiguration;
    }

    /**
     * Cache for {@link ResponseHolder} objects
     */
    private static final CacheAccess<Integer, ResponseHolder> responseCache = JCS.getInstance("talo-response");

    /**
     * An exchange between multiple threads for {@link Future}s associated with a request.
     */
    private static final ConcurrentMap<Integer, CompletableFuture<ResponseHolder>> futureExchange = new ConcurrentHashMap<Integer, CompletableFuture<ResponseHolder>>(8, 0.9f, 2);

    /**
     * Attribute key to be used on the {@link io.netty.channel.ChannelHandlerContext} for accessing the
     * request identifier.
     */
    private static final AttributeKey<Integer> requestKey = AttributeKey.valueOf("requestKey");

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
//        LOGGER.debug("Channel active. ({})", ctx.channel().id().asShortText());
        // this handler is a decision point on how to proceed, so we have to read the request to make a decision
        // on how to proceed
        ctx.read();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        LOGGER.debug("Read channel requesting {} ({})", HttpRequest.class.cast(msg).getUri(), ctx.channel().id().asShortText());

        // get the identifier for this quest
        final Integer requestIdentifier = requestIdentifier(HttpRequest.class.cast(msg));

        // see if we have any information about this request
        final ICacheElement<Integer, ResponseHolder> cacheElement = responseCache.getCacheElement(requestIdentifier);

        // only continue forwarding the request if we can't fulfill it by other means
        if (!sendExistingResponse(ctx, requestIdentifier, cacheElement)) {
            LOGGER.debug("Fulfilling {} ({})", HttpRequest.class.cast(msg).getUri(), ctx.channel().id().asShortText());
            ctx.attr(requestKey).setIfAbsent(requestIdentifier);
            ctx.pipeline().addLast(new FrontendHandler(configuration));
            super.channelRead(ctx, msg);
        }
    }

    /**
     * Attempts to send a response to the client (via cache or request coalescing)
     *
     * @param ctx
     * @param requestIdentifier
     * @param cacheElement
     * @return true if the client will receive a response, false otherwise
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private boolean sendExistingResponse(final ChannelHandlerContext ctx, final Integer requestIdentifier, final ICacheElement<Integer, ResponseHolder> cacheElement) throws ExecutionException, InterruptedException {
        if (sendCachedResponse(ctx, requestIdentifier, cacheElement) || sendCoalescedRequest(ctx, requestIdentifier)) {
            return true;
        }

        return false;
    }

    /**
     * Attempts to coalesce with an existing request.
     *
     * @param ctx
     * @param requestIdentifier
     * @return true if successful, false otherwise
     */
    private boolean sendCoalescedRequest(final ChannelHandlerContext ctx, final Integer requestIdentifier) {
        // synchronized so there is never more than one thread adding its future for coalescing
        synchronized (futureExchange) {
            // if nobody is fulfilling the request already, add a new future
            if (!futureExchange.containsKey(requestIdentifier)) {
                futureExchange.put(requestIdentifier, new CompletableFuture<ResponseHolder>());
                return false;
            }
        }

        LOGGER.debug("Waiting for existing request. ({})", ctx.channel().id().asShortText());
        // get the future for the request already being fulfilled and write its response when it is completed
        final CompletableFuture<ResponseHolder> future = futureExchange.get(requestIdentifier);
        future.thenAccept(new Consumer<ResponseHolder>() {
            @Override
            public void accept(ResponseHolder responseHolder) {
                writeResponse(ctx, responseHolder);
            }
        });

        return true;
    }

    /**
     * Attempts to send a cached response to the client.
     *
     * @param ctx
     * @param requestIdentifier
     * @param cacheElement
     * @return true if successful, false otherwise
     */
    private boolean sendCachedResponse(final ChannelHandlerContext ctx, final Integer requestIdentifier, final ICacheElement<Integer, ResponseHolder> cacheElement) {
        // no cacheElement means we definitely don't have any cache for this
        if (cacheElement != null) {
            final ResponseHolder responseHolder = cacheElement.getVal();

            if (responseHolder != null) {
                LOGGER.debug("Returning cached response. ({})", ctx.channel().id().asShortText());
                writeResponse(ctx, responseCache.get(requestIdentifier));
                return true;
            }
        }

        return false;
    }

    /**
     * Write out the response to the channel and close it on success.
     *
     * @param ctx
     * @param responseHolder
     */
    private void writeResponse(final ChannelHandlerContext ctx, final ResponseHolder responseHolder) {
        LOGGER.debug("Writing response. ({})", ctx.channel().id().asShortText());
        ctx.writeAndFlush(responseHolder.getResponse()).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isDone() && future.isSuccess()) {
                    LOGGER.debug("Response written. Closing channel. ({})", ctx.channel().id().asShortText());
                    ctx.channel().close();
                } else {
                    LOGGER.debug("Response failed. ({})", ctx.channel().id().asShortText());
                    future.isCancelled();
                }
            }
        });
    }

    /**
     * Create a unique hash for a {@link HttpRequest}
     *
     * @param httpRequest
     * @return
     */
    private int requestIdentifier(final HttpRequest httpRequest) {
        int hashCode = httpRequest.getMethod().hashCode();
        hashCode = hashCode ^ httpRequest.getUri().hashCode();
        hashCode = hashCode ^ httpRequest.getProtocolVersion().hashCode();
        for (Map.Entry<String, String> header : httpRequest.headers()) {
            hashCode = hashCode ^ (header.getKey().hashCode() + header.getValue().hashCode());
        }

        return hashCode;
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
        final Integer requestIdentifier = ctx.attr(requestKey).get();

        DateTime expiration = DateTime.now().plusMinutes(5);
        final ResponseHolder responseHolder = new ResponseHolder(requestIdentifier, FullHttpResponse.class.cast(msg), expiration);
        if (shouldCache()) {
            responseCache.put(requestIdentifier, responseHolder);
        }

        futureExchange.remove(requestIdentifier).complete(responseHolder);

        super.write(ctx, responseHolder.getResponse(), promise);
    }

    private boolean shouldCache() {
        return true;
    }
}

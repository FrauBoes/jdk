/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.net.httpserver;

import sun.net.httpserver.DelegatingHttpExchange;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A handler which is invoked to process HTTP exchanges. Each
 * HTTP exchange is handled by one of these handlers.
 *
 * @apiNote The methods {@link #handleOrElse(Predicate, HttpHandler)} and
 * {@link #adaptRequest(UnaryOperator)} are conveniences to complement and adapt
 * a given handler, in order to extend or modify its functionality.
 * <p>
 * Example of a complemented and adapted {@code HttpHandler}:
 * <pre>    {@code var uri = URI.create("https://someuri");
 *    var handler = new SomePutHandler();
 *    var fallbackHandler = new SomeOtherHandler();
 *    var finalHandler = handler.handleOrElse(r -> r.getRequestMethod().equals("PUT"), fallbackHandler)
 *                              .adaptRequest(r -> r.with(r.getRequestURI().resolve(uri));
 *    var s = HttpServer.create(new InetSocketAddress(8080), 10, "/", finalHandler);
 *    s.start();
 * }</pre>
 *
 * @since 1.6
 */
public interface HttpHandler {
    /**
     * Handle the given request and generate an appropriate response.
     * See {@link HttpExchange} for a description of the steps
     * involved in handling an exchange.
     *
     * @param exchange the exchange containing the request from the
     *                 client and used to send the response
     * @throws NullPointerException if exchange is {@code null}
     */
    public abstract void handle (HttpExchange exchange) throws IOException;

    /**
     * Complements this handler with a fallback handler. Any request that
     * matches the {@code requestTest} is handled by the fallback handler. All
     * other requests are handled by this handler.
     *
     * @param requestTest     a predicate given the request
     * @param fallbackHandler another handler
     * @return a handler
     * @throws NullPointerException if any argument is null
     * @since 17
     */
    default HttpHandler handleOrElse(Predicate<Request> requestTest,
                                     HttpHandler fallbackHandler) {
        Objects.requireNonNull(fallbackHandler);
        Objects.requireNonNull(requestTest);
        return exchange -> {
            if (requestTest.test(exchange))
                handle(exchange);
            else fallbackHandler.handle(exchange);
        };
    }

    /**
     * Allows this handler to inspect and adapt the request state, before
     * handling the exchange. The {@code Request} returned by the operator
     * will be the effective request state of the exchange when handled.
     *
     * @param requestOperator the request operator
     * @return a handler
     * @throws NullPointerException if the argument is null
     * @since 17
     */
    default HttpHandler adaptRequest(UnaryOperator<Request> requestOperator) {
        Objects.requireNonNull(requestOperator);
        return exchange -> {
            var request = requestOperator.apply(exchange);
            var newExchange = new DelegatingHttpExchange(exchange) {
                @Override
                public URI getRequestURI() { return request.getRequestURI(); }

                @Override
                public String getRequestMethod() { return request.getRequestMethod(); }

                @Override
                public Headers getRequestHeaders() { return request.getRequestHeaders(); }
            };
            this.handle(newExchange);
        };
    }
}

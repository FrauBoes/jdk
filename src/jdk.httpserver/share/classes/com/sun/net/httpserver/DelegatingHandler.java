/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import sun.net.httpserver.UnmodifiableHeaders;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A set of convenience handlers that can delegate the {@code HttpExchange}.
 * <p>
 * A {@code DelegatingHandler} instance can be obtained from one of the static
 * factory methods. If no {@code HttpHandler} is passed, the returned handler
 * handles all exchanges by sending a client error response code. Otherwise, the
 * returned handler delegates all exchanges to the passed {@code HttpHandler}.
 * <p>
 * The instance methods are conveniences to retrieve common types of handlers
 * that can be used individually or in combination. The methods are based around
 * the central elements of a HTTP request: <ul>
 * <li>{@link #delegatingIfMethod(Predicate, HttpHandler)} returns a handler that
 * delegates based on the HTTP method of the incoming request,</li>
 * <li>{@link #inspectingURI(UnaryOperator)} returns a handler that can inspect
 * and replace the request {@code URI},</li>
 * <li>{@link #addingRequestHeader(String, String)} returns a handler that can
 * add a header to the request,</li>
 * <li>{@link #discardingRequestBody()} returns a handler that preemptively
 * reads and discards the request body.</li>
 *
 * @apiNote This class offers conveniences to create common types of
 * {@code HttpHandlers}. The instance methods can be chained together in order
 * to modify or extend the functionality of a given handler.
 * <p>
 * Example of a chained {@code DelegatingHandler}:
 * <pre>    {@code var someURI = URI.create("https://someuri");
 *    var aHandler = new SomeHeadAndGetHandler();
 *    var anotherHandler = new SomePutHandler();
 *    var combinedHandler = DelegatingHandler.of()
 *        .delegatingIfMethod(m -> m.equals("HEAD") || m.equals("GET"), aHandler)
 *        .delegatingIfMethod(m -> m.equals("PUT"), anotherHandler)
 *        .inspectingURI(uri -> uri.resolve(someURI))
 *        .addingRequestHeader("someHeader", "someValue");
 *    var s = HttpServer.create(new InetSocketAddress(8080), 10, "/", combinedHandler);
 *    s.start();
 * }</pre>
 * <p>
 * Example of a handler that always discards the request body:
 * <pre>    {@code var handler = DelegatingHandler.of(aHandler).discardingRequestBody();
 *    var server = HttpServer.create(new InetSocketAddress(8080), 10), "/", handler);
 *    server.start();
 * }</pre>
 *
 * @since 17
 */
public final class DelegatingHandler implements HttpHandler {
    private final HttpHandler handler;

    private DelegatingHandler(HttpHandler handler) {
        this.handler = handler;
    }

    /**
     * Forwards the exchange to this handler.
     *
     * @param exchange the exchange containing the request from the
     *                 client and used to send the response
     * @throws IOException
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        handler.handle(exchange);
    }

    /**
     * Returns a handler that always sends the HTTP {@code 404 Not Found} client
     * response code.
     *
     * @return a handler
     */
    public static DelegatingHandler of() {
        return new DelegatingHandler(exchange -> {
            try (exchange) {
                exchange.sendResponseHeaders(404, -1);
            }
        });
    }

    /**
     * Returns a handler that forwards all exchanges to the passed {@code handler}.
     *
     * @param handler a handler to forward to
     * @return a handler
     * @throws NullPointerException if handler is null
     */
    public static DelegatingHandler of(HttpHandler handler) {
        Objects.requireNonNull(handler);
        return DelegatingHandler.of().delegatingIfMethod(p -> true, handler);
    }

    /**
     * Returns a handler that delegates the exchange to an {@code otherHandler}
     * if the request method of the exchange matches the {@code methodTest}.
     * All other exchanges are forwarded to this handler.
     *
     * @param otherHandler another handler
     * @param methodTest   a predicate given the request method
     * @return a handler
     * @throws NullPointerException if any argument is null
     */
    public final DelegatingHandler delegatingIfMethod(Predicate<String> methodTest,
                                                      HttpHandler otherHandler) {
        Objects.requireNonNull(otherHandler);
        Objects.requireNonNull(methodTest);
        HttpHandler handler = exchange -> {
            if (methodTest.test(exchange.getRequestMethod()))
                otherHandler.handle(exchange);
            else handle(exchange);
        };
        return new DelegatingHandler(handler);
    }

    /**
     * Returns a handler that allows inspection (and possible replacement) of
     * the request URI, before forwarding to this handler. The {@code URI}
     * returned by the operator will be the effective uri of the exchange when
     * forwarded.
     *
     * @param uriOperator the URI operator
     * @return a handler
     * @throws NullPointerException if any argument is null
     */
    public final DelegatingHandler inspectingURI(UnaryOperator<URI> uriOperator) {
        Objects.requireNonNull(uriOperator);
        HttpHandler handler = exchange -> {
            var uri = uriOperator.apply(exchange.getRequestURI());
            var newExchange = new DelegatingHttpExchange(exchange) {
                @Override
                public URI getRequestURI() {
                    return uri;
                }
            };
            this.handle(new DelegatingHttpExchange(newExchange));
        };
        return new DelegatingHandler(handler);
    }

    /**
     * Returns a handler that adds a request header and value to the exchange
     * before forwarding to this handler.
     *
     * @param name  the header name
     * @param value the header value
     * @return a handler
     * @throws NullPointerException if any argument is null
     */
    public final DelegatingHandler addingRequestHeader(String name,
                                                       String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        HttpHandler handler = exchange -> {
            ((UnmodifiableHeaders) exchange.getRequestHeaders()).map.add(name, value);
            this.handle(exchange);
        };
        return new DelegatingHandler(handler);
    }

    /**
     * Returns a handler that reads and discards any request body before
     * forwarding the exchange to this handler.
     *
     * @return a discarding handler
     */
    public final DelegatingHandler discardingRequestBody() {
        HttpHandler handler = exchange -> {
            try (InputStream is = exchange.getRequestBody()) {
                is.readAllBytes();
            }
            this.handle(exchange);
        };
        return new DelegatingHandler(handler);
    }
}

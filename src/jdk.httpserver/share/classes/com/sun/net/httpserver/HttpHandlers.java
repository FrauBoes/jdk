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

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class HttpHandlers {

    private HttpHandlers() {}

    // Factories

    public static HttpHandler create() {
        return exchange -> {
            try (exchange) {
                exchange.sendResponseHeaders(404, -1);
            }
        };
    }

    public static HttpHandler create(HttpHandler handler) {
        return delegatingIf(p -> true, handler, create());
    }

    // Composites

    /**
     * Returns a handler that delegates the exchange to a {@code targetHandler}
     * if the request method of the exchange matches the {@code methodTest}.
     * All other exchanges are forwarded to the {@code fallbackHandler}.
     *
     * @param methodTest      the predicate given the request method
     * @param targetHandler   the handler to delegate exchanges to
     * @param fallbackHandler the handler to forward exchanges to
     * @return a handler
     * @throws NullPointerException if any argument is null
     */
    public static HttpHandler delegatingIf(Predicate<String> methodTest,
                                           HttpHandler targetHandler,
                                           HttpHandler fallbackHandler) {
        Objects.requireNonNull(targetHandler);
        Objects.requireNonNull(fallbackHandler);
        Objects.requireNonNull(methodTest);
        return exchange -> {
            if (methodTest.test(exchange.getRequestMethod()))
                targetHandler.handle(exchange);
            else fallbackHandler.handle(exchange);
        };
    }

    /**
     * Returns a handler that reads and discards any request body before
     * forwarding the exchange to the {@code targetHandler}.
     *
     * @param targetHandler the handler to forward exchanges to
     * @return a discarding handler
     */
    public static HttpHandler discardingRequestBody(HttpHandler targetHandler) {
        Objects.requireNonNull(targetHandler);
        return exchange -> {
            try (InputStream is = exchange.getRequestBody()) {
                is.readAllBytes();
            }
            targetHandler.handle(exchange);
        };
    }

    /**
     * Returns a handler that adds a request header and value to the exchange
     * before forwarding it to the {@code targetHandler}.
     *
     * @param name  the header name
     * @param value the header value
     * @param targetHandler the handler to forward exchanges to
     * @return a handler
     * @throws NullPointerException if any argument is null
     */
    public static HttpHandler addingRequestHeader(String name,
                                                  String value,
                                                  HttpHandler targetHandler) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        Objects.requireNonNull(targetHandler);
        return exchange -> {
            ((UnmodifiableHeaders) exchange.getRequestHeaders()).map.add(name, value);
            targetHandler.handle(exchange);
        };
    }

    /**
     * Returns a handler that allows inspection (and possible replacement) of
     * the request URI, before forwarding the exchange to the {@code targetHandler}.
     * The {@code URI} returned by the operator will be the effective uri of
     * the exchange when forwarded.
     *
     * @param uriOperator   the URI operator
     * @param targetHandler the handler to forward exchanges to
     * @return a handler
     * @throws NullPointerException if any argument is null
     */
    public static HttpHandler inspectingURI(UnaryOperator<URI> uriOperator,
                                             HttpHandler targetHandler) {
        Objects.requireNonNull(uriOperator);
        Objects.requireNonNull(targetHandler);
        return exchange -> {
            var newUri = uriOperator.apply(exchange.getRequestURI());
            var newExchange = new DelegatingHttpExchange(exchange) {
                @Override
                public URI getRequestURI() {
                    return newUri;
                }
            };
            targetHandler.handle(newExchange);
        };
    }
}

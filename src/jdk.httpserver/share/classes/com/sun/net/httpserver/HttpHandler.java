/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * A handler which is invoked to process HTTP exchanges. Each
 * HTTP exchange is handled by one of these handlers.
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
    public abstract void handle(HttpExchange exchange) throws IOException;

    /**
     * Returns a handler that delegates the exchange to an {@code otherHandler}
     * if the request method of the exchange matches the {@code methodTest}.
     * All other exchanges are forwarded to this handler.
     *
     * @param otherHandler another handler
     * @param methodTest a predicate given the request method
     * @return a handler
     * @throws NullPointerException if any argument is null
     */
    default HttpHandler delegating(HttpHandler otherHandler,
                                Predicate<String> methodTest) {
        Objects.requireNonNull(otherHandler);
        Objects.requireNonNull(methodTest);
        return exchange -> {
            if (methodTest.test(exchange.getRequestMethod()))
                otherHandler.handle(exchange);
            else
                this.handle(exchange);
        };
    }

    /**
     * Returns a handler that reads and discards any request body before
     * forwarding the exchange to this handler.
     *
     * @return a discarding handler
     */
    default HttpHandler discardingRequestBody() {
        return exchange -> {
            try (InputStream is = exchange.getRequestBody()) {
                is.readAllBytes();
            }
            this.handle(exchange);
        };
    }

    /**
     * Returns a handler that adds a request header and value to the exchange
     * before forwarding to this handler.
     *
     * @param name the header name
     * @param value the header value
     * @return a handler
     * @throws NullPointerException if any argument is null
     */
    default HttpHandler addingRequestHeader(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        return exchange -> {
            ((UnmodifiableHeaders)exchange.getRequestHeaders()).map.add(name, value);
            this.handle(exchange);
        };
    }

    /**
     * Returns a handler that allows inspection (and possible replacement) of
     * the request URI, before forwarding to this handler. The {@code URI}
     * returned by the operator will be the effective uri of the exchange when
     * forwarded.
     *
     * Question: Maybe we should put restrictions on the type transforms?
     *
     * @param uriOperator the URI operator
     * @return a handler
     * @throws NullPointerException if any argument is null
     */
    default HttpHandler inspectingURI(UnaryOperator<URI> uriOperator) {
        Objects.requireNonNull(uriOperator);
        return exchange -> {
            var uri = uriOperator.apply(exchange.getRequestURI());
            var newExchange = new DelegatingHttpExchange(exchange) {
                @Override
                public URI getRequestURI() {
                    return uri;
                }
            };
            this.handle(new DelegatingHttpExchange(newExchange));
        };
    }
}

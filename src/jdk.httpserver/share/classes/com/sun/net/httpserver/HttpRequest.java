/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import sun.net.httpserver.UnmodifiableHeaders;

import java.net.URI;

/**
 * Immutable HTTP Request state
 */
public interface HttpRequest {

    URI getRequestURI();

    String getRequestMethod();

    Headers getRequestHeaders();

//    default HttpRequest with(URI requestURI) {
//        final HttpRequest r = this;
//        return new HttpRequest() {
//            @Override
//            public URI getRequestURI() { return requestURI; }
//
//            @Override
//            public String getRequestMethod() { return r.getRequestMethod(); }
//
//            @Override
//            public Headers getRequestHeaders() {
//                return r.getRequestHeaders();
//            }
//        };
//    }
//
//    default HttpRequest with(String requestMethod) {
//        final HttpRequest r = this;
//        return new HttpRequest() {
//            @Override
//            public URI getRequestURI() {
//                return r.getRequestURI();
//            }
//
//            @Override
//            public String getRequestMethod() { return requestMethod; }
//
//            @Override
//            public Headers getRequestHeaders() {
//                return r.getRequestHeaders();
//            }
//        };
//    }

    default HttpRequest with(String headerName, String headerValue) {
        final HttpRequest r = this;
        return new HttpRequest() {
            @Override
            public URI getRequestURI() { return r.getRequestURI(); }

            @Override
            public String getRequestMethod() { return r.getRequestMethod(); }

            @Override
            public Headers getRequestHeaders() {
                ((UnmodifiableHeaders) r.getRequestHeaders())
                        .map.add(headerName, headerValue);
                return r.getRequestHeaders();
            }
        };
    }

    default HttpRequest with(Headers requestHeaders) {
        final HttpRequest r = this;
        return new HttpRequest() {
            @Override
            public URI getRequestURI() { return r.getRequestURI(); }

            @Override
            public String getRequestMethod() { return r.getRequestMethod(); }

            @Override
            public Headers getRequestHeaders() { return requestHeaders; }};
    }
}

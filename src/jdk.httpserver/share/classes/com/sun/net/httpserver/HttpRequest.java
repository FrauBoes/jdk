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

    default HttpRequest of(HttpExchange e) {
        return new HttpRequest() {
            @Override
            public URI getRequestURI() {
                return e.getRequestURI();
            }

            @Override
            public String getRequestMethod() {
                return e.getRequestMethod();
            }

            @Override
            public Headers getRequestHeaders() {
                return e.getRequestHeaders();
            }
        };
    }

    //        return exchange -> {
    //            var uri = uriOperator.apply(exchange.getRequestURI());
    //            var newExchange = new DelegatingHttpExchange(exchange) {
    //                @Override
    //                public URI getRequestURI() {
    //                    return uri;
    //                }
    //            };
    //            this.handle(new DelegatingHttpExchange(newExchange));

    //    default HttpHandler addingRequestHeader(String name, String value) {
    //        Objects.requireNonNull(name);
    //        Objects.requireNonNull(value);
    //        return exchange -> {
    //            ((UnmodifiableHeaders) exchange.getRequestHeaders())
    //                    .map.add(name, value);
    //            this.handle(exchange);


    default HttpRequest with(URI uri) {
        return new HttpRequest() {
            @Override
            public URI getRequestURI() {
                return uri;
            }

            @Override
            public String getRequestMethod() {
                return this.getRequestMethod();
            }

            @Override
            public Headers getRequestHeaders() {
                return this.getRequestHeaders();
            }
        };
    }

    default HttpRequest with(String requestMethod) {
        return new HttpRequest() {
            @Override
            public URI getRequestURI() {
                return this.getRequestURI();
            }

            @Override
            public String getRequestMethod() {
                return requestMethod;
            }

            @Override
            public Headers getRequestHeaders() {
                return this.getRequestHeaders();
            }
        };
    }

    default HttpRequest with(Headers requestHeaders) {
        return new HttpRequest() {
            @Override
            public URI getRequestURI() {
                return this.getRequestURI();
            }

            @Override
            public String getRequestMethod() {
                return this.getRequestMethod();
            }

            @Override
            public Headers getRequestHeaders() {
                return requestHeaders;
            }
        };
    }
}

/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import sun.net.httpserver.FileServerHandler;
import sun.net.httpserver.OutputFilter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

/**
 * This class provides a simple HTTP file server and its components.
 * <p>
 * The simple file server is composed of a HttpServer that is bound to the
 * wildcard address and the given port, a HttpHandler that displays the static
 * content of the given directory in HTML, and a Filter that provides basic
 * logging of the HttpExchange.
 * <p>
 * Each component can be retrieved for reuse and extension via the static
 * methods provided. In the case of the HttpHandler, the provided instance can
 * be wrapped by a custom HttpHandler to handle request methods other than HEAD
 * and GET.
 * <p>
 * A default implementation of the simple HTTP file server is provided via the
 * main entry point of the jdk.httpserver module.
 */
public final class SimpleFileServer {
    public enum Output {
        NONE, DEFAULT, VERBOSE;
    }

    /**
     * Creates a HttpServer with a HttpHandler that displays the static content
     * of the given directory in HTML.
     * The server is bound to the wildcard address and the given port, and comes
     * with a filter that logs information about the HttpExchange to System.out.
     *
     * @param port the port number
     * @param root the root directory to be served, must be an absolute path
     * @return a HttpServer
     * @throws UncheckedIOException
     */
    public static HttpServer createServer(int port, Path root, Output output) {
        try {
            return output.equals(Output.NONE)  // don't add OutputFilter
               ? HttpServer.create(new InetSocketAddress(port), 0, "/",
                   new FileServerHandler(root))
               : HttpServer.create(new InetSocketAddress(port), 0, "/",
               new FileServerHandler(root), new OutputFilter(System.out, output.equals(Output.VERBOSE)));
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Creates a HttpHandler that displays the static content of the given
     * directory in HTML. Only HEAD and GET requests can be handled.
     *
     * @param root the root directory to be served, must be an absolute path
     * @return a HttpHandler
     */
    public static HttpHandler createFileHandler(Path root) {
        return new FileServerHandler(root);
    }

    /**
     * Creates a Filter that logs information about a HttpExchange to the given
     * OutputStream.
     *
     * @param out the OutputStream to log to
     * @param verbose if true, include headers in logging information
     * @return a Filter
     */
    public static Filter createFilter(OutputStream out, boolean verbose) {
        return new OutputFilter(out, verbose);
    }
}

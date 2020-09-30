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
 * A class that provides a simple HTTP file server and its components.
 * <p>
 * The simple file server is composed of <ul>
 *     <li>a {@link HttpServer HttpServer} that is bound to the wildcard
 *     address and the given port,</li>
 *     <li>a {@link HttpHandler HttpHandler} that displays the static content of
 *     the given directory in HTML,</li>
 *     <li>a {@link Filter Filter} that outputs information about the
 *     {@link HttpExchange HttpExchange}.</li></ul>
 * <p>
 * Each component can be retrieved for reuse and extension via the static
 * methods provided. In the case of the {@code HttpHandler}, the provided instance
 * can be wrapped by a custom {@code HttpHandler} to handle request methods other
 * than HEAD and GET.
 * <p>
 * A default implementation of the simple HTTP file server is provided via the
 * main entry point of the {@code jdk.httpserver} module.
 */
public final class SimpleFileServer {
    public enum Output {
        NONE, DEFAULT, VERBOSE
    }

    /**
     * Creates a {@code HttpServer} with a {@code HttpHandler} that displays
     * the static content of the given directory in HTML.
     * <p>
     * The server is bound to the wildcard address and the given port. An optional
     * {@code Filter} can be specified via the {@code output} argument. If
     * {@link Output#NONE Output.NONE} is passed, no {@code Filter} is added.
     * Otherwise a {@code Filter} is added that prints information about the
     * {@code HttpExchange} to {@code System.out}, with either
     * {@linkplain Output#NONE default} or {@linkplain Output#VERBOSE verbose} output.
     *
     * @param port the port number
     * @param root the root directory to be served, must be an absolute path
     * @param output the verbosity of the OutputFilter
     * @return a HttpServer
     * @throws UncheckedIOException
     */
    public static HttpServer createServer(int port, Path root, Output output) {
        try {
            return output.equals(Output.NONE)
               ? HttpServer.create(new InetSocketAddress(port), 0, root,
                   new FileServerHandler(root))
               : HttpServer.create(new InetSocketAddress(port), 0, root,
               new FileServerHandler(root), new OutputFilter(System.out, output.equals(Output.VERBOSE)));
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Creates a {@code HttpHandler} that displays the static content of the given
     * directory in HTML. Only HEAD and GET requests can be handled.
     *
     * @param root the root directory to be served, must be an absolute path
     * @return a HttpHandler
     */
    public static HttpHandler createFileHandler(Path root) {
        return new FileServerHandler(root);
    }

    /**
     * Creates a {@code Filter} that prints information about the {@code HttpExchange}
     * to the given {@code OutputStream}.
     *
     * @param out the OutputStream to print to
     * @param verbose if true, include request and response headers in the output.
     * @return a Filter
     */
    public static Filter createOutputFilter(OutputStream out, boolean verbose) {
        return new OutputFilter(out, verbose);
    }
}

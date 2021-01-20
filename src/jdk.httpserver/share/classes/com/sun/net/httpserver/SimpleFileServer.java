/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

/**
 * A simple HTTP file server and its components, provided for educational purposes.
 * <p>
 * The simple file server is composed of <ul>
 * <li>a {@link HttpServer HttpServer} that is bound to the wildcard
 * address and a given port,</li>
 * <li>a {@link HttpHandler HttpHandler} that displays the static content of
 * a given directory in HTML,</li>
 * <li>an optional {@link Filter Filter} that outputs information about a
 * {@link HttpExchange HttpExchange}. The output format is specified by a
 * {@link OutputLevel OutputLevel}.</li></ul>
 * <p>
 * The components can be retrieved for reuse and extension via the static
 * methods provided.
 * <p><b>Simple file server</b><p>
 * {@link #createFileServer(int, Path, OutputLevel)} returns a
 * {@link HttpServer HttpServer} that is a simple out-of-the-box file server.
 * It comes with a handler that displays the static content of the given
 * directory in HTML, and an optional filter that prints output about the
 * {@code HttpExchange} to {@code System.out}.
 * <p>
 * Example of a simple file server:
 * <pre>    {@code var server = SimpleFileServer.createFileServer(8080, Path.of("/some/path"), OutputLevel.DEFAULT);
 *    server.start();}</pre>
 * <p><b>File server handler</b><p>
 * {@link #createFileHandler(Path)} returns a {@code HttpHandler} that
 * displays the static content of the given directory in HTML. The handler can
 * serve directory listings and files, the content type of a file is determined
 * on a {@linkplain #createFileHandler(Path) best-guess} basis. The handler
 * supports only HEAD and GET requests; to handle request methods other than
 * HEAD and GET, the handler instance can be complemented by the server's other
 * handlers, or it can conditionally delegate the exchange to another {@code HttpHandler}.
 * <p>Example of a complemented file handler:
 * <pre>    {@code class PutHandler implements HttpHandler {
 *        @Override
 *        public void handle(HttpExchange exchange) throws IOException {
 *            // handle PUT request
 *        }
 *    }
 *    ...
 *    var handler = SimpleFileServer.createFileHandler(Path.of("/some/path"));
 *    var server = HttpServer.create(new InetSocketAddress(8080), 10, "/browse/", handler);
 *    server.createContext("/store/", new PutHandler());
 *    server.start();
 *    }</pre>
 * <p>
 * Example of a delegating file handler
 * <pre>    {@code var handler = DelegatingHandler.of(
 *                  SimpleFileServer.createFileHandler(Path.of("/some/path")))
 *                      .delegatingIfMethod(method -> method.equals("PUT"), new PutHandler());
 *    var server = HttpServer.create(new InetSocketAddress(8080), 10, "/some/context/", handler);
 *    server.start();
 *    ...
 *    }</pre>
 * <p><b>Output filter</b><p>
 * {@link #createOutputFilter(OutputStream, OutputLevel)} returns a {@code Filter}
 * that prints output about a {@code HttpExchange} to the given
 * {@code OutputStream}. The output format is specified by the
 * {@link OutputLevel outputLevel}.
 * <p>
 * Example of an output filter:
 * <pre>    {@code var filter = SimpleFileServer.createOutputFilter(System.out, OutputLevel.VERBOSE);
 *    var server = HttpServer.create(new InetSocketAddress(8080), 10, "/store/", new PutHandler(), filter);
 *    server.start();}</pre>
 * <p>
 * A default implementation of the simple HTTP file server is provided via the
 * main entry point of the {@code jdk.httpserver} module, which can be used on
 * the command line as such:
 * <p>
 * <pre>    {@code java -m jdk.httpserver [-p port] [-d directory] [-o none|default|verbose]}</pre>
 */
public final class SimpleFileServer {

    private static final Function<String, String> MIME_TABLE =
            s -> URLConnection.getFileNameMap().getContentTypeFor(s);

    private SimpleFileServer() {
    }

    /**
     * Describes the output produced by a {@code HttpExchange}.
     */
    public enum OutputLevel {
        /**
         * Used to specify no output.
         */
        NONE,
        /**
         * Used to specify output in the default format.
         * <p>
         * The default format is based on the <a href='https://www.w3.org/Daemon/User/Config/Logging.html#common-logfile-format'>Common Logfile Format</a>.
         * and includes the following information:
         * <p>
         * {@code remotehost rfc931 authuser [date] "request" status bytes}
         * <p>
         * Example:
         * <p>
         * {@code 127.0.0.1 - - [22/Jun/2000:13:55:36 -0700] "GET /example.txt HTTP/1.0" 200 -}
         * <p>
         * Note: The fields {@code rfc931}, {@code authuser} and {@code bytes}
         * are not captured in the implementation and are always represented as
         * {@code '-'}.
         */
        DEFAULT,
        /**
         * Used to specify output in the verbose format.
         * <p>
         * Additional to the information provided by the
         * {@linkplain OutputLevel#DEFAULT default} format, the verbose format
         * includes the request and response headers of the {@code HttpExchange}.
         */
        VERBOSE
    }

    /**
     * Creates a {@code HttpServer} with a {@code HttpHandler} that displays
     * the static content of the given directory in HTML.
     * <p>
     * The server is bound to the wildcard address and the given port. The handler
     * is mapped to the URI path "/" via a {@code HttpContext}. It only supports
     * HEAD and GET requests and serves directory listings, html and text files.
     * Other MIME types are supported on a best-guess basis.
     * <p>
     * An optional {@code Filter} that prints information about the
     * {@code HttpExchange} to {@code System.out} can be specified via the
     * {@linkplain OutputLevel outputLevel} argument. If
     * {@link OutputLevel#NONE OutputLevel.NONE} is passed, no {@code Filter} is
     * added. Otherwise a {@code Filter} is added with either
     * {@linkplain OutputLevel#DEFAULT default} or
     * {@linkplain OutputLevel#VERBOSE verbose} output format.
     *
     * @param port        the port number
     * @param root        the root directory to be served, must be an absolute path
     * @param outputLevel the output about a http exchange
     * @return a HttpServer
     * @throws UncheckedIOException
     * @throws NullPointerException if any of the object arguments is null
     */
    public static HttpServer createFileServer(int port,
                                              Path root,
                                              OutputLevel outputLevel) {
        Objects.requireNonNull(root);
        Objects.requireNonNull(outputLevel);
        try {
            return outputLevel.equals(OutputLevel.NONE)
                    ? HttpServer.create(new InetSocketAddress(port), 0, "/",
                    FileServerHandler.create(root, MIME_TABLE))
                    : HttpServer.create(new InetSocketAddress(port), 0, "/",
                    FileServerHandler.create(root, MIME_TABLE),
                    new OutputFilter(System.out, outputLevel));
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Creates a {@code HttpHandler} that displays the static content of the
     * given directory in HTML.
     * <p>
     * The handler supports only HEAD and GET requests and can serve directory
     * listings and files. Content types are supported on a best-guess basis.
     *
     * @param root the root directory to be served, must be an absolute path
     * @return a HttpHandler
     * @implNote The content type of a file is guessed by calling
     * {@link java.net.FileNameMap#getContentTypeFor(String)} on the
     * {@link URLConnection#getFileNameMap() mimeTable} found.
     * @throws NullPointerException if the argument is null
     */
    public static HttpHandler createFileHandler(Path root) {
        Objects.requireNonNull(root);
        return FileServerHandler.create(root, MIME_TABLE);
    }

    /**
     * Creates a {@code Filter} that prints output about a {@code HttpExchange}
     * to the given {@code OutputStream}.
     * <p>
     * The output format is specified by the {@link OutputLevel outputLevel}.
     *
     * @param out         the OutputStream to print to
     * @param outputLevel the output about a http exchange
     * @return a Filter
     * @implNote An {@link IllegalArgumentException} is thrown if
     * {@link OutputLevel#NONE OutputLevel.NONE} is passed. It is recommended
     * to not use a filter in this case.
     * @throws NullPointerException if any argument is null
     */
    public static Filter createOutputFilter(OutputStream out,
                                            OutputLevel outputLevel) {
        Objects.requireNonNull(out);
        Objects.requireNonNull(outputLevel);
        return new OutputFilter(out, outputLevel);
    }
}

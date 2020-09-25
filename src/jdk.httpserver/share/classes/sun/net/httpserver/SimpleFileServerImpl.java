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

package sun.net.httpserver;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Executors;

/**
 * This class provides a simple HTTP file server created on a directory.
 * <p>
 * It is composed of a HttpHandler that displays the static content of the given
 * directory in HTML, a HttpServer that serves the content on the wildcard
 * address and the given port, and a filter that logs to System.out.
 * <p>
 * The implementation is provided via the main entry point of the jdk.httpserver
 * module.
 */
final class SimpleFileServerImpl {
    private static final Path ROOT = Path.of(".");
    private static final int PORT = 8000;

    /**
     * Starts a simple HTTP file server created on a directory.
     *
     * @param args the command line options
     */
    public static void main(String[] args) {
        int port = PORT;
        Path root = ROOT;
        boolean verbose = false;
        boolean silent = false;
        Iterator<String> options = Arrays.asList(args).iterator();
        try {
            while (options.hasNext()) {
                String option = options.next();
                switch (option) {
                    case "-p" -> port = Integer.parseInt(options.next());
                    case "-d" -> root = Path.of(options.next());
                    case "-v" -> verbose = true;
                    case "-s" -> silent = true;
                    default -> throw new AssertionError();
                }
            }
            if (silent && verbose) {
                throw new AssertionError();
            }
        } catch (NoSuchElementException | AssertionError e) {
            System.out.println("usage: java -m jdk.httpserver [-p port] [-d directory] [-s | -v]");
            System.exit(1);
        }

        try {
            var server = silent ? // don't add log filter
                HttpServer.create(new InetSocketAddress(port), 0,"/",
                    new FileServerHandler(root))
               : HttpServer.create(new InetSocketAddress(port), 0,"/",
               new FileServerHandler(root), new OutputFilter(System.out, verbose)) ;
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            System.out.printf("Serving %s on port %d ...\n", root, port);
        } catch (IOException ioe) {
            System.out.println("Connection failed: " + ioe.getMessage());
        }
    }
}

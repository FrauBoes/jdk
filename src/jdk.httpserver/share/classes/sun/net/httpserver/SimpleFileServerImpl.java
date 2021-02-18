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

package sun.net.httpserver;

import com.sun.net.httpserver.SimpleFileServer;
import com.sun.net.httpserver.SimpleFileServer.OutputLevel;

import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * A class that provides a simple HTTP file server to serve the content of
 * a given directory.
 * <p>
 * It is composed of a HttpHandler that displays the static content of the given
 * directory in HTML, a HttpServer that serves the content on the given address
 * and port, and an OutputFilter that prints information about the HttpExchange
 * to System.out.
 * <p>
 * Unless specified as arguments, the default values are:<ul>
 * <li>address wildcard address</li>
 * <li>port 8000</li>
 * <li>directory current working directory</li>
 * <li>outputLevel default</li></ul>
 * <p>
 * The implementation is provided via the main entry point of the jdk.httpserver
 * module.
 */
public final class SimpleFileServerImpl {
    private static final InetAddress ADDR = null;
    private static final int PORT = 8000;
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final OutputLevel OUTPUT_LEVEL = OutputLevel.DEFAULT;
    private static PrintWriter out = new PrintWriter(System.out, true);

    /**
     * Starts a simple HTTP file server created on a directory.
     *
     * @param args the command line options
     */
    public static void main(String[] args) {
        InetAddress addr = ADDR;
        int port = PORT;
        Path root = ROOT;
        OutputLevel outputLevel = OUTPUT_LEVEL;
        Iterator<String> options = Arrays.asList(args).iterator();
        try {
            while (options.hasNext()) {
                String option = options.next();
                switch (option) {
                    case "-b" -> addr = InetAddress.getByName(options.next());
                    case "-p" -> port = Integer.parseInt(options.next());
                    case "-d" -> root = Path.of(options.next());
                    case "-o" -> outputLevel = Enum.valueOf(OutputLevel.class,
                            options.next().toUpperCase(Locale.ROOT));
                    default -> throw new AssertionError();
                }
            }
        } catch (AssertionError | UnknownHostException | NumberFormatException e) {
            out.println("usage: java -m jdk.httpserver [-b bind address] "
                    + "[-p port] [-d directory] [-o none|default|verbose]");
            System.exit(1);
        }

        try {
            var socketAddr = new InetSocketAddress(addr, port);
            var server = SimpleFileServer.createFileServer(
                    socketAddr, root, outputLevel);
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
            out.printf("Serving %s on %s:%d ...\n", root, socketAddr.getHostString(), port);
        } catch (UncheckedIOException e) {
            out.println("Connection failed: " + e.getMessage());
        }
    }

    public static void mainForProvider(PrintWriter o, PrintWriter err, String[] args) {
        out = o;
        main(args);
    }
}

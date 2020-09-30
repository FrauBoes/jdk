/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A basic HTTP file server handler for static content.
 * <p>
 * Must be given an absolute pathname to the directory to be served.
 * Can only handle HEAD and GET requests. Directory listings, html and text files
 * can be served, other MIME types are supported on a best-guess basis.
 */
public final class FileServerHandler implements HttpHandler {
    private final Path ROOT;

    public FileServerHandler(Path root) {
        if (!Files.exists(root)) {
            throw new IllegalArgumentException("Path does not exist " + root);
        }
        ROOT = root;
    }

    public void handle(HttpExchange t) throws IOException {
        try (InputStream is = t.getRequestBody()) {
            is.readAllBytes();
        }
        Headers respHeaders = t.getResponseHeaders();
        URI uri = t.getRequestURI();
        String file = uri.getPath();

        var path = ROOT.resolve(file);
        if (!Files.exists(path)) {
            notFound(t, file);
            return;
        }

        String method = t.getRequestMethod();
        if (method.equals("HEAD")) {
            respHeaders.set("Content-Length", Long.toString(Files.size(path)));
            t.sendResponseHeaders(200, -1);
            t.close();
        } else if (!method.equals("GET")) {
            t.sendResponseHeaders(405, -1);
            t.close();
            return;
        }

        // best-guess MIME type
        String type = getMediaType(file);
        respHeaders.set("Content-Type", type);

        if (Files.isDirectory(path)) {
            if (!file.endsWith("/")) {
                redirect(t);
                return;
            }
            respHeaders.set("Content-Type", "text/html");
            Path index = getIndex(path);
            if (index != null) {
                serveFile(t, index);
            } else {
                listFiles(t, path, file);
            }
        } else {
            serveFile(t, path);
        }
    }

    private Path getIndex(Path path) {
        if (Files.exists(path.resolve("index.html"))) {
            return path.resolve("index.html");
        }
        if (Files.exists(path.resolve("index.htm"))) {
            return path.resolve("index.htm");
        }
        return null;
    }

    private void serveFile(HttpExchange t, Path path) throws IOException {
        t.sendResponseHeaders(200, 0);
        try (InputStream fis = Files.newInputStream(path);
             OutputStream os = t.getResponseBody()) {
            fis.transferTo(os);
        }
    }

    private String getMediaType(String file) {
        String type = URLConnection.getFileNameMap().getContentTypeFor(file);
        return type != null ? type : "text/html";
    }

    private static String sanitize(String file) {
        var sb = new StringBuilder();
        file.chars().forEach(c -> sb.append(switch (c) {
            case (int) '&' -> "&amp;";
            case (int) '<' -> "&lt;";
            case (int) '>' -> "&gt;";
            case (int) '"' -> "&quot;";
            case (int) '\'' -> "&#x27;";
            case (int) '/' -> "&#x2F;";
            default -> Character.toString(c);
        }));
        return sb.toString();
    }

    private void listFiles(HttpExchange t, Path path, String file)
            throws IOException {
        t.sendResponseHeaders(200, 0);
        try (OutputStream os = t.getResponseBody();
             PrintStream ps = new PrintStream(os)) {
            ps.println("<!DOCTYPE html>");
            ps.println("<html>");
            ps.println("<body>");
            ps.println("<h2>Directory listing for " + sanitize(file) + "</h2>");
            ps.println("<ul>");
            Files.list(path).map(p -> path.toUri().relativize(p.toUri()).toASCIIString())
                    .forEach(uri ->
                            ps.println("<li><a href=\"" + uri + "\">" + sanitize(uri) + "</a></li>"));
            ps.println("</ul><p><hr>");
            ps.println("</body>");
            ps.println("</html>");
            ps.flush();
        }
    }

    private void redirect(HttpExchange t) throws IOException {
        Headers reqHeaders = t.getRequestHeaders();
        Headers respHeaders = t.getResponseHeaders();
        URI uri = t.getRequestURI();
        String host = reqHeaders.getFirst("Host");
        var location = "http://" + host + uri.getPath() + "/";
        respHeaders.set("Content-Type", "text/html");
        respHeaders.set("Location", location);
        try (t) {
            t.sendResponseHeaders(301, -1);
        }
    }

    private void notFound(HttpExchange t, String file) throws IOException {
        t.getResponseHeaders().set("Content-Type", "text/html");
        t.sendResponseHeaders(404, 0);
        try (t; OutputStream os = t.getResponseBody()) {
            var s = "<h2>File not found</h2>" + sanitize(file) + "<p>";
            os.write(s.getBytes());
        }
    }
}

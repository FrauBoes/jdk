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

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Filter that provides basic logging of an HttpExchange
 */
public final class LogFilter extends Filter {
    private final PrintStream ps;
    private final DateTimeFormatter df;

    public LogFilter(OutputStream out) {
        ps = new PrintStream(out);
        df = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
    }

    /**
     * The filter's implementation, which is invoked by the server
     */
    public void doFilter(HttpExchange t, Chain chain) throws IOException {
        chain.doFilter(t);
        String s = t.getLocalAddress() + " ";
        s += "[" + LocalDateTime.now().format(df) + "] ";
        s += "\"" + t.getRequestMethod() + " " + t.getRequestURI() + "\" ";
        s += t.getResponseCode() + " " + t.getRemoteAddress();
        ps.println(s);
    }

    public String description() {
        return "Request logger";
    }
}

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
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Filter that provides basic logging output for a HttpExchange.
 * <p>
 * The non-verbose output format is based on the Common Log Format, see
 * https://www.w3.org/Daemon/User/Config/Logging.html#common-logfile-format.
 * It includes the following information:
 * <p>
 * Common Logfile Format
 * remotehost rfc931 authuser [date] "request" status bytes
 * <p>
 * Example
 * 127.0.0.1 - jane [10/Oct/2000:13:55:36 -0700] "GET /apache_pb.gif HTTP/1.0" 200 2326
 * <p>
 * The verbose output format additionally includes the request and response headers
 * of the HttpExchange.
 */
public final class OutputFilter extends Filter {
	 private static final DateTimeFormatter formatter =
		 DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");
	 private final PrintStream printStream;
	 private final Boolean verbose;

	 public OutputFilter (OutputStream os, boolean verbose) {
		  printStream = new PrintStream(os);
		  this.verbose = verbose;
	 }

	 /**
	  * The filter's implementation, which is invoked by the server
	  */
	 public void doFilter (HttpExchange t, Chain chain) throws IOException {
		  chain.doFilter(t);
		  String s = t.getLocalAddress().getAddress().getHostAddress() + " "
			  + "- - "    // rfc931 and authuser
			  + "[" + OffsetDateTime.now().format(formatter) + "] "
			  + "\"" + t.getRequestMethod() + " " + t.getRequestURI() + "\" "
			  + t.getResponseCode() + " "
			  + "-";    // bytes
		  printStream.println(s);
		  if (verbose) {
				logHeaders(t.getRequestHeaders(), ">");
				logHeaders(t.getResponseHeaders(), "<");
		  }
	 }

	 private void logHeaders (Headers headers, String sign) {
		  headers.forEach((name, values) -> {
				var sb = new StringBuilder();
				values.forEach(v -> sb.append(v).append(" "));
				printStream.println(sign + " " + name + ": " + sb.toString());
		  });
		  printStream.println(sign);
	 }

	 public String description () {
		  return "HttpExchange OutputFilter (verbose: " + verbose + ")";
	 }
}

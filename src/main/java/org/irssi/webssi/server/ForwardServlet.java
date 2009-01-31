package org.irssi.webssi.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that forwards requests for events.json to irssi.
 * Only used for testing; to run in hosted mode.
 */
public class ForwardServlet extends HttpServlet {

	private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		URL url = new URL("http://localhost:38444/events.json");

		// read incoming request
		BufferedReader webIn = request.getReader();
		String inputLine;
		StringBuilder inputRequest = new StringBuilder();
		while ((inputLine = webIn.readLine()) != null)
			inputRequest.append(inputLine);

		// open connection
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(request.getMethod());
		conn.setAllowUserInteraction(false);
		conn.setDoOutput(true);
		
		// copy request headers
		for (Enumeration<?> enu = request.getHeaderNames(); enu.hasMoreElements();) {
			String pName = (String) enu.nextElement();
			String pValu = request.getHeader(pName);
			if (!pName.toUpperCase().startsWith("HOST")) {
				conn.setRequestProperty(pName, pValu);
			} else {
				conn.setRequestProperty(pName, url.getHost() + ":" + url.getPort());
			}
		}

		// copy request data
		OutputStream rawOutStream = conn.getOutputStream();
		PrintWriter pw = new PrintWriter(rawOutStream);
		pw.print(inputRequest);
		pw.flush();
		pw.close();

		// copy response headers
		for (int i=1; conn.getHeaderFieldKey(i) != null; i++) {
			response.setHeader(conn.getHeaderFieldKey(i), conn.getHeaderField(i));
		}
		
		int responseCode = conn.getResponseCode();
		if (responseCode == 200) {
			// read response
			BufferedReader rdr = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			PrintWriter out = new PrintWriter(response.getOutputStream());
			StringBuffer sb = new StringBuffer();
			String line;
			while ((line = rdr.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
			
			// write response
			out.print(sb.toString());
			out.flush();
			out.close();
		} else {
			// copy error
			response.sendError(responseCode, conn.getResponseMessage());
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		processRequest(request, response);
	}
}

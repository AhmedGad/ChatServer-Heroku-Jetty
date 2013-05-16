import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

class TCPServer extends HttpServlet {

	private static final long serialVersionUID = -7823703173356571077L;

	ArrayList<String> lst = new ArrayList<String>();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			String tempstr = req.getParameter("req");
			PrintWriter out = resp.getWriter();
			out.println("request: " + tempstr);

			if (tempstr.equals("0")) {
				out = resp.getWriter();
				for (int i = 0; i < lst.size(); i++)
					out.println("message nu: " + (i + 1) + " : " + lst.get(i));
			} else if (tempstr.equals("1")) {
				out = resp.getWriter();
				out.println("list size = " + lst.size());
			}
			out.close();
		} catch (Exception e) {
			PrintWriter out = resp.getWriter();
			out.println("server work");
			out.close();
		}
	}

	String Password = "androidelteety";

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		InputStream in = req.getInputStream();
		PrintWriter out = resp.getWriter();

		byte[] buffer = new byte[1000];
		int read;
		String tempstr = "";
		while ((read = in.read(buffer)) != -1)
			tempstr += new String(buffer, 0, read, "ISO-8859-1");

		out.println(tempstr);
		out.println("Please Enter Password");

		tempstr = "";
		req.getInputStream();
		while ((read = in.read(buffer)) != -1)
			tempstr += new String(buffer, 0, read, "ISO-8859-1");

		out = resp.getWriter();
		out.println(tempstr);
		out.println("registered succsessfully");

		// if (tempstr.toString().equals("clear")) {
		// lst.clear();
		// System.gc();
		// } else {
		// int len = new Integer(tempstr.toString());
		// String tmp = "";
		// for (int i = 0; i < 30; i++) {
		// tmp += (char) ('a' + (int) (Math.random() * 26));
		// }
		// for (int i = 0; i < len; i++)
		// lst.add(i + " " + tmp);
		// }

	}

	public static void main(String[] args) throws Exception {
		Server server = new Server(Integer.valueOf(System.getenv("PORT")));
		ServletContextHandler context = new ServletContextHandler(
				ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		context.addServlet(new ServletHolder(new TCPServer()), "/*");
		server.start();
		server.join();
	}
}
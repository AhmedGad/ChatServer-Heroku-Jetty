import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

@SuppressWarnings("serial")
class TCPServer extends HttpServlet {

	ArrayList<String> reg = new ArrayList<String>();
	ArrayList<String> hosts = new ArrayList<String>();
	ArrayList<Game> games = new ArrayList<Game>();

	boolean running[] = new boolean[max];
	int otherPlayer[] = new int[max];
	String messages[] = new String[max];
	static Object locks[] = new Object[100];

	static {
		for (int i = 0; i < locks.length; i++)
			locks[i] = new Object();
	}

	static String Password = "androidelteety";
	static int notifycnt = 0, max = 100;

	class Game {
		int pl1, pl2;

		public Game(int pl1, int pl2) {
			this.pl1 = pl1;
			this.pl2 = pl2;
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		PrintWriter out = resp.getWriter();
		try {
			String tempstr = req.getParameter("show");
			if (tempstr != null) {

				if (tempstr.equals("regs")) {
					out.println("num of registrars: " + reg.size());
					out.println("waiting for notify:" + notifycnt);
					for (int i = 0; i < reg.size(); i++)
						out.println(reg.get(i));
				} else if (tempstr.equals("hosts")) {
					out.println("num of hosts: " + hosts.size());
					for (int i = 0; i < hosts.size(); i++)
						out.println(reg.get(i));
				} else if (tempstr.equals("games")) {
					out.println("num of live games: " + games.size());
					for (int i = 0; i < games.size(); i++)
						out.println(reg.get(games.get(i).pl1) + " --> "
								+ reg.get(games.get(i).pl2));
				}
			} else
				out.println("server works!");
		} catch (Exception e) {
			out.println("exception");
			out.println(e.toString());
		}

		out.close();
	}

	void run(PrintWriter out, int id) {
		notifycnt++;
		while (running[id]) {
			if (messages[id] != null) {
				out.print(messages[id]);
				messages[id] = null;
			}
		}
		notifycnt--;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		InputStream in = req.getInputStream();
		final PrintWriter out = resp.getWriter();

		byte[] buffer = new byte[1000];
		int read;
		String s = "";
		while ((read = in.read(buffer)) != -1)
			s += new String(buffer, 0, read);

		StringTokenizer tok = new StringTokenizer(s);
		if (tok.nextToken().equals(Password)) {
			synchronized (reg) {
				String operation = tok.nextToken();
				if (operation.equals("register")) {
					String regName = tok.nextToken();
					if (reg.contains(regName)) {
						out.println("name already exists");
						out.println("registered failed");
					} else {
						reg.add(regName);
						out.println("registered succsessfully");
					}
				} else if (operation.equals("unregister")) {
					String regName = tok.nextToken();
					if (!reg.contains(regName)) {
						out.println("no such name exists");
						out.println("unregister failed");
					} else {
						reg.remove(regName);
						out.println("unregistered succsessfully");
					}
				} else if (operation.equals("host")) {
					synchronized (hosts) {
						String regName = tok.nextToken();
						if (!reg.contains(regName)) {
							out.println("no such name exists");
							out.println("unregister failed");
						} else if (hosts.contains(regName)) {
							out.println("you are already in the host list");
						} else {
							hosts.add(regName);
							out.println("host done succsessfully");
							final int id = reg.indexOf(regName);
							otherPlayer[id] = -1;
							running[id] = true;
							new Thread(new Runnable() {
								public void run() {
									TCPServer.this.run(out, id);
								}
							});
						}
					}
				} else if (operation.equals("connect")) {
					String me = tok.nextToken(), host = tok.nextToken();
					if (!reg.contains(me) || !reg.contains(host)) {
						out.println("no such name exists");
						out.println("connect failed");
					} else if (hosts.contains(req)) {
						out.println("you are already in the host list");
					} else {
						final Game g;
						synchronized (games) {
							games.add(g = new Game(reg.indexOf(host), reg
									.indexOf(me)));
							otherPlayer[g.pl1] = otherPlayer[g.pl2];
							otherPlayer[g.pl2] = otherPlayer[g.pl1];
						}
						running[g.pl2] = true;
						new Thread(new Runnable() {
							public void run() {
								TCPServer.this.run(out, g.pl2);
							}
						});
					}
				} else if (operation.equals("disconnect")) {
					String name = tok.nextToken();
					if (reg.contains(name)) {
						running[reg.indexOf(name)] = false;
						out.println("diconnected succsessfully");
					} else
						out.println("diconnect failed");
				} else if (operation.equals("reconnect")) {
					String name = tok.nextToken();
					if (reg.contains(name)) {
						final int id = reg.indexOf(name);
						running[id] = true;
						new Thread(new Runnable() {
							public void run() {
								TCPServer.this.run(out, id);
							}
						});
						out.println("reconnected succsessfully");
					} else
						out.println("reconnect failed");
				} else if (operation.equals("message")) {
					String to = tok.nextToken();
					if (reg.contains(to))
						messages[reg.indexOf(to)] = tok.nextToken();
				}
			}
		} else {
			out.println("incorrect password");
		}
		out.close();
		in.close();
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
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

	static int notifycnt = 0, max = 100;
	static boolean running[] = new boolean[max];
	static int otherPlayer[] = new int[max];
	@SuppressWarnings("unchecked")
	static ArrayList<String> messages[] = new ArrayList[max];
	static Object locks[] = new Object[max];

	static {
		for (int i = 0; i < max; i++) {
			locks[i] = new Object();
			otherPlayer[i] = -1;
			messages[i] = new ArrayList<String>();
		}
	}

	static String Password = "androidelteety";

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
						out.println(hosts.get(i));
				} else if (tempstr.equals("games")) {
					out.println("num of live games: " + games.size());
					for (int i = 0; i < games.size(); i++)
						out.println(reg.get(games.get(i).pl1) + " --> "
								+ reg.get(games.get(i).pl2));
				} else if (tempstr.equals("messages")) {
					for (int i = 0; i < reg.size(); i++) {
						out.println(reg.get(i) + " :");
						for (int j = 0; j < messages[i].size(); j++) {
							out.println(reg.get(j));
						}
						out.print("\n");
					}
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
		out.flush();
		while (running[id]) {
			if (messages[id].size() > 0) {
				for (int i = 0; i < messages[id].size(); i++)
					out.println(messages[id].get(i));
				messages[id].clear();
				out.flush();
			}
			try {
				synchronized (locks[id]) {
					locks[id].wait();
				}
			} catch (InterruptedException e) {
				out.println("exception on lock wait");
				out.flush();
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
					running[reg.indexOf(regName)] = false;
					messages[reg.indexOf(regName)].clear();
					synchronized (locks[reg.indexOf(regName)]) {
						locks[reg.indexOf(regName)].notifyAll();
					}
					if (hosts.contains(regName))
						hosts.remove(regName);
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
						out.print(" ");
						out.flush();
						final int id = reg.indexOf(regName);
						otherPlayer[id] = -1;
						running[id] = true;
						run(out, id);
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
					games.add(g = new Game(reg.indexOf(host), reg.indexOf(me)));
					hosts.remove(host);
					messages[reg.indexOf(host)].add(me + " " + "connected");
					synchronized (locks[reg.indexOf(host)]) {
						locks[reg.indexOf(host)].notifyAll();
					}
					out.print(" ");
					out.flush();
					otherPlayer[g.pl1] = otherPlayer[g.pl2];
					otherPlayer[g.pl2] = otherPlayer[g.pl1];
					running[g.pl2] = true;
					run(out, g.pl2);
				}
			} else if (operation.equals("disconnect")) {
				String name = tok.nextToken();
				if (reg.contains(name)) {
					if (hosts.contains(name))
						hosts.remove(name);
					out.println(running[reg.indexOf(name)]);
					running[reg.indexOf(name)] = false;
					synchronized (locks[reg.indexOf(name)]) {
						locks[reg.indexOf(name)].notifyAll();
					}
					out.println(running[reg.indexOf(name)]);
					out.println("diconnected succsessfully");
				} else
					out.println("diconnect failed");
			} else if (operation.equals("reconnect")) {
				String name = tok.nextToken();
				if (reg.contains(name)) {
					final int id = reg.indexOf(name);
					running[id] = false;
					synchronized (locks[id]) {
						locks[id].notifyAll();
					}
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					running[id] = true;
					out.print(" ");
					out.flush();
					run(out, id);
				} else
					out.println("reconnect failed");
			} else if (operation.equals("message")) {
				String from = tok.nextToken();
				if (reg.contains(from)) {
					int myid = reg.indexOf(from);
					out.println("" + otherPlayer[myid]);
					if (otherPlayer[myid] > -1) {
						int to = otherPlayer[myid];
						String tmp = "";
						while (tok.hasMoreTokens())
							tmp += tok.nextToken();
						messages[to].add(tmp);
						synchronized (locks[to]) {
							locks[to].notifyAll();
						}
						out.println("message sent successfully");
					}
				}
			} else if (operation == "endGame") {
				String me = tok.nextToken();
				if (reg.contains(me)) {
					int from = reg.indexOf(me), to;
					if (otherPlayer[from] > -1) {
						to = otherPlayer[from];
						otherPlayer[from] = -1;
						otherPlayer[to] = -1;
						running[from] = false;
						running[to] = false;
						messages[from].clear();
						messages[to].clear();
						synchronized (locks[from]) {
							locks[from].notifyAll();
						}
						synchronized (locks[to]) {
							locks[to].notifyAll();
						}
					}
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
package org.example;

import java.net.URI;
import java.util.concurrent.Executor;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.proxy.AsyncProxyServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.HttpCookieStore;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class Main {

	private static void printUsage() {
		System.out.println("usage: simple_reverse_proxy target_url port");
	}
	
	public static void main(String[] args) {
		if (args.length < 2) {
			printUsage();
			System.exit(1);
		}
		
		try {
			String argTargetUrl = args[0];
			int argPort = Integer.parseInt(args[1]);
		
			Server server = new Server(argPort);
			ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
			contextHandler.setContextPath("/");
			server.setHandler(contextHandler);

			contextHandler.addServlet(new ServletHolder(new AsyncProxyServlet(){
				private String targetUrl;
				
				public AsyncProxyServlet setTargetUrl(String s) {
					this.targetUrl = s;
					return this;
				}
				
				@Override
				protected HttpClient createHttpClient() throws ServletException
				{
					ServletConfig config = getServletConfig();
					SslContextFactory scf = new SslContextFactory();
					//	scf.setTrustAll(true);
					HttpClient client = new HttpClient(scf);
					client.setFollowRedirects(false);
					client.setCookieStore(new HttpCookieStore.Empty());

					Executor executor;
					String value = config.getInitParameter("maxThreads");
					if (value == null || "-".equals(value))
					{
						executor = (Executor)getServletContext().getAttribute("org.eclipse.jetty.server.Executor");
						if (executor==null)
							throw new IllegalStateException("No server executor for proxy");
					}
					else
					{
						QueuedThreadPool qtp= new QueuedThreadPool(Integer.parseInt(value));
						String servletName = config.getServletName();
						int dot = servletName.lastIndexOf('.');
						if (dot >= 0)
							servletName = servletName.substring(dot + 1);
						qtp.setName(servletName);
						executor=qtp;
					}

		            client.setExecutor(executor);

		            value = config.getInitParameter("maxConnections");
		            if (value == null)
		                value = "256";
		            client.setMaxConnectionsPerDestination(Integer.parseInt(value));

		            value = config.getInitParameter("idleTimeout");
		            if (value == null)
		                value = "30000";
		            client.setIdleTimeout(Long.parseLong(value));

		            value = config.getInitParameter("timeout");
		            if (value == null)
		                value = "60000";
		            this.setTimeout(Long.parseLong(value));

		            value = config.getInitParameter("requestBufferSize");
		            if (value != null)
		                client.setRequestBufferSize(Integer.parseInt(value));

		            value = config.getInitParameter("responseBufferSize");
		            if (value != null)
		                client.setResponseBufferSize(Integer.parseInt(value));
		            
		            try
		            {
		                client.start();

		                // Content must not be decoded, otherwise the client gets confused
		                client.getContentDecoderFactories().clear();

		                return client;
		            }
		            catch (Exception x)
		            {
		                throw new ServletException(x);
		            }
					
				}
				
			    @Override
			    protected URI rewriteURI(HttpServletRequest request) {
			    	return URI.create(this.targetUrl + request.getRequestURI());
			    }
			}.setTargetUrl(argTargetUrl)), "/*");

	        server.start();
	        server.join();
		} catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

}

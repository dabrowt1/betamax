package co.freeside.betamax.proxy.jetty

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.util.Promise

import javax.servlet.http.HttpServletResponse
import java.nio.channels.SocketChannel
import javax.servlet.http.HttpServletRequest
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.proxy.ConnectHandler

class CustomConnectHandler extends ConnectHandler {

	int sslPort

	CustomConnectHandler(Handler handler, int sslPort) {
		super(handler)
		this.sslPort = sslPort
	}

	@Override
	protected void handleConnect(Request baseRequest, HttpServletRequest request, HttpServletResponse response, String serverAddress) {
		super.handleConnect(baseRequest, request, response, '127.0.0.1')
	}
}

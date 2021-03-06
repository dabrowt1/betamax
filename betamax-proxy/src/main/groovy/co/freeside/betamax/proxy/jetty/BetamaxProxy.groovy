package co.freeside.betamax.proxy.jetty

import java.util.logging.Logger
import javax.servlet.http.*
import co.freeside.betamax.handler.*
import co.freeside.betamax.message.Response
import co.freeside.betamax.message.servlet.ServletRequestAdapter
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import static java.util.logging.Level.SEVERE
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR

class BetamaxProxy extends AbstractHandler {

	private HttpHandler handlerChain

	private static final Logger log = Logger.getLogger(BetamaxProxy.name)

	@Override
	void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
		def betamaxRequest = new ServletRequestAdapter(request)
		try {
			def betamaxResponse = handlerChain.handle(betamaxRequest)
			sendResponse(betamaxResponse, response)
		} catch (HandlerException e) {
			log.log SEVERE, 'exception in proxy processing', e
			response.sendError(e.httpStatus, e.message)
		} catch (Exception e) {
			log.log SEVERE, 'error recording HTTP exchange', e
			response.sendError(SC_INTERNAL_SERVER_ERROR, e.message)
		}
	}

	HttpHandler leftShift(HttpHandler httpHandler) {
		handlerChain = httpHandler
		handlerChain
	}

	private static void sendResponse(Response betamaxResponse, HttpServletResponse response) {
		response.status = betamaxResponse.status
		betamaxResponse.headers.each { name, value ->
			value.split(/,\s*/).each {
				response.addHeader(name.toString(), it)
			}
		}
		if (betamaxResponse.hasBody()) {
			response.outputStream.withStream { stream ->
				stream << betamaxResponse.bodyAsBinary
			}
		}
		response.flushBuffer()
	}

}

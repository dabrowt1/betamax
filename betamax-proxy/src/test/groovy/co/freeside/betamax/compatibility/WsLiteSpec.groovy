package co.freeside.betamax.compatibility

import co.freeside.betamax.*
import co.freeside.betamax.proxy.jetty.SimpleServer
import co.freeside.betamax.util.server.*
import org.junit.Rule
import spock.lang.*
import wslite.rest.RESTClient
import static co.freeside.betamax.Headers.X_BETAMAX
import static co.freeside.betamax.TapeMode.WRITE_ONLY
import static co.freeside.betamax.util.FileUtils.newTempDir
import static java.net.HttpURLConnection.HTTP_OK
import static org.apache.http.HttpHeaders.VIA

class WsLiteSpec extends Specification {

	@Shared @AutoCleanup('deleteDir') File tapeRoot = newTempDir('tapes')
	@Rule ProxyRecorder recorder = new ProxyRecorder(tapeRoot: tapeRoot, defaultMode: WRITE_ONLY)
	@Shared @AutoCleanup('stop') SimpleServer endpoint = new SimpleServer()

	void setupSpec() {
		endpoint.start(EchoHandler)
	}

	@Betamax(tape = 'wslite spec')
	void 'can record a connection made with WsLite'() {
		given: 'a properly configured wslite instance'
		def http = new RESTClient(endpoint.url)

		when: 'a request is made'
		def response = http.get(path: '/', proxy: recorder.proxy)

		then: 'the request is intercepted'
		response.statusCode == HTTP_OK
		response.headers[VIA] == 'Betamax'
		response.headers[X_BETAMAX] == 'REC'
	}
}

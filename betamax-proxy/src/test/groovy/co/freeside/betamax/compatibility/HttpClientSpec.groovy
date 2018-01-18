package co.freeside.betamax.compatibility

import co.freeside.betamax.*
import co.freeside.betamax.httpclient.*
import co.freeside.betamax.proxy.jetty.SimpleServer
import co.freeside.betamax.util.server.*
import org.apache.http.HttpHost
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.*
import org.junit.Rule
import spock.lang.*
import static co.freeside.betamax.TapeMode.WRITE_ONLY
import static co.freeside.betamax.util.FileUtils.newTempDir
import static java.net.HttpURLConnection.HTTP_OK
import static org.apache.http.HttpHeaders.VIA
import static org.apache.http.HttpStatus.SC_OK
import static org.apache.http.conn.params.ConnRoutePNames.DEFAULT_PROXY

class HttpClientSpec extends Specification {

	@AutoCleanup('deleteDir') File tapeRoot = newTempDir('tapes')
	@Rule ProxyRecorder recorder = new ProxyRecorder(tapeRoot: tapeRoot, defaultMode: WRITE_ONLY)
	@Shared @AutoCleanup('stop') SimpleServer endpoint = new SimpleServer()

	void setupSpec() {
		endpoint.start(EchoHandler)
	}

	@Timeout(10)
	@Betamax(tape = 'http client spec')
	void 'proxy intercepts HTTPClient connections when using ProxySelectorRoutePlanner'() {
		given:
		def http = new DefaultHttpClient()
		BetamaxRoutePlanner.configure(http)

		when:
		def request = new HttpGet(endpoint.url)
		def response = http.execute(request)

		then:
		response.statusLine.statusCode == HTTP_OK
		response.getFirstHeader(VIA)?.value == 'Betamax'
	}

	@Timeout(10)
	@Betamax(tape = 'http client spec')
	void 'proxy intercepts HTTPClient connections when explicitly told to'() {
		given:
		def http = new DefaultHttpClient()
		http.params.setParameter(DEFAULT_PROXY, new HttpHost('localhost', recorder.proxyPort, 'http'))

		when:
		def request = new HttpGet(endpoint.url)
		def response = http.execute(request)

		then:
		response.statusLine.statusCode == HTTP_OK
		response.getFirstHeader(VIA)?.value == 'Betamax'
	}

	@Timeout(10)
	@Betamax(tape = 'http client spec')
	void 'proxy automatically intercepts SystemDefaultHttpClient connections'() {
		given:
		def http = new SystemDefaultHttpClient()

		when:
		def request = new HttpGet(endpoint.url)
		def response = http.execute(request)

		then:
		response.statusLine.statusCode == HTTP_OK
		response.getFirstHeader(VIA)?.value == 'Betamax'
	}
}

package co.freeside.betamax.compatibility

import co.freeside.betamax.*
import co.freeside.betamax.proxy.jetty.SimpleServer
import co.freeside.betamax.util.server.*
import org.junit.Rule
import spock.lang.*
import static co.freeside.betamax.TapeMode.WRITE_ONLY
import static co.freeside.betamax.util.FileUtils.newTempDir
import static java.net.HttpURLConnection.HTTP_OK
import static org.apache.http.HttpHeaders.VIA
import static org.apache.http.HttpStatus.SC_OK

class HttpURLConnectionSpec extends Specification {

	@AutoCleanup('deleteDir') File tapeRoot = newTempDir('tapes')
	@Rule Recorder recorder = new ProxyRecorder(tapeRoot: tapeRoot, defaultMode: WRITE_ONLY)
	@Shared @AutoCleanup('stop') SimpleServer endpoint = new SimpleServer()

	void setupSpec() {
		endpoint.start(EchoHandler)
	}

	@Timeout(10)
	@Betamax(tape = 'http url connection spec')
	void 'proxy intercepts URL connections'() {
		given:
		HttpURLConnection connection = new URL(endpoint.url).openConnection()
		connection.connect()

		expect:
		connection.responseCode == HTTP_OK
		connection.getHeaderField(VIA) == 'Betamax'

		cleanup:
		connection.disconnect()
	}
}

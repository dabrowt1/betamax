package co.freeside.betamax


import co.freeside.betamax.util.httpbuilder.BetamaxRESTClient
import groovyx.net.http.*
import org.junit.Rule
import spock.lang.*
import static java.net.HttpURLConnection.HTTP_OK
import static org.apache.http.HttpHeaders.VIA

@Unroll
class SmokeSpec extends Specification {

	@Rule Recorder recorder = new ProxyRecorder()

	@Shared RESTClient http = new BetamaxRESTClient()

	@Betamax(tape = 'smoke spec')
	void '#type response data'() {
		when:
		HttpResponseDecorator response = http.get(uri: uri)

		then:
		response.status == HTTP_OK

		where:
		type   | uri
		'html' | 'http://grails.org/'
		'json' | 'http://api.twitter.com/1/statuses/public_timeline.json?count=3&include_entities=true'
		'xml'  | 'http://feeds.feedburner.com/wondermark'
		'png'  | 'http://media.xircles.codehaus.org/_projects/groovy/_logos/small.png'
		'css'  | 'http://d297h9he240fqh.cloudfront.net/cache-1633a825c/assets/views_one.css'
	}

	@Issue('https://github.com/robfletcher/betamax/issues/52')
	@Betamax(tape = 'ocsp')
	void 'OCSP messages'() {
		when:
		HttpResponseDecorator response = http.post(uri: 'http://ocsp.ocspservice.com/public/ocsp')

		then:
		response.status == HTTP_OK
		response.data.bytes.length == 2529
	}

	@Issue(['https://github.com/robfletcher/betamax/issues/61', 'http://jira.codehaus.org/browse/JETTY-1533'])
	@Betamax(tape = 'smoke spec')
	void 'can cope with URLs that do not end in a slash'() {
		given:
		def uri = 'http://groovy-lang.org'

		when:
		HttpResponseDecorator response = http.get(uri: uri)

		then:
		response.status == HTTP_OK
		response.getFirstHeader(VIA).value == 'Betamax'
	}
}

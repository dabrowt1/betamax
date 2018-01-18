package co.freeside.betamax.httpclient

import co.freeside.betamax.Betamax
import co.freeside.betamax.Recorder
import org.junit.Rule
import org.springframework.http.HttpStatus
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

class RestTemplateSpec extends Specification {
  @Rule
  Recorder recorder = new Recorder()
  def http = new BetamaxHttpClient(recorder)

  @Betamax(tape = 'rest template')
  void 'can use RestTemplate'() {
    given:
    def restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(http))

    when:
    def response = restTemplate.getForEntity("http://groovy-lang.org", String.class)

    then:
    response.statusCode == HttpStatus.OK
  }
}

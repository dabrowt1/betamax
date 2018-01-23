package co.freeside.betamax.tape

import co.freeside.betamax.encoding.GzipEncoder
import co.freeside.betamax.message.*
import co.freeside.betamax.util.message.*
import spock.lang.*
import static co.freeside.betamax.TapeMode.*
import static org.apache.http.HttpHeaders.*
import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED

@Stepwise
class TapeSpec extends Specification {

	@Shared Tape tape = new MemoryTape(name: 'tape_spec')
	Request getRequest = new BasicRequest('GET', 'http://icanhascheezburger.com/')
	Response plainTextResponse = new BasicResponse(status: 200, reason: 'OK', body: new GzipEncoder().encode('O HAI!', 'UTF-8'))

	void setup() {
		plainTextResponse.addHeader(CONTENT_TYPE.toString(), 'text/plain;charset=UTF-8')
		plainTextResponse.addHeader(CONTENT_LANGUAGE.toString(), 'en-GB')
		plainTextResponse.addHeader(CONTENT_ENCODING.toString(), 'gzip')
	}

	void cleanup() {
		tape.mode = READ_WRITE
	}

	void 'reading from an empty tape throws an exception'() {
		when: 'an empty tape is played'
		tape.play(getRequest)

		then: 'an exception is thrown'
		thrown IllegalStateException
	}

	void 'can write an HTTP interaction to a tape'() {
		when: 'an HTTP interaction is recorded to tape'
		tape.record(getRequest, plainTextResponse)

		then: 'the size of the tape increases'
		tape.size() == old(tape.size()) + 1
		def interaction = tape.interactions[-1]

		and: 'the request data is correctly stored'
		interaction.request.method == getRequest.method
		interaction.request.uri == getRequest.uri

		and: 'the response data is correctly stored'
		interaction.response.status == plainTextResponse.status
		interaction.response.body == 'O HAI!'
		interaction.response.headers[CONTENT_TYPE] == plainTextResponse.getHeader(CONTENT_TYPE)
		interaction.response.headers[CONTENT_LANGUAGE] == plainTextResponse.getHeader(CONTENT_LANGUAGE)
		interaction.response.headers[CONTENT_ENCODING] == plainTextResponse.getHeader(CONTENT_ENCODING)
	}
	
	void 'can overwrite a recorded interaction'() {
		when: 'a recording is made'
		tape.record(getRequest, plainTextResponse)

		then: 'the tape size does not increase'
		tape.size() == old(tape.size())

		and: 'the previous recording was overwritten'
		tape.interactions[-1].recorded > old(tape.interactions[-1].recorded)
	}

	void 'seek does not match a request for a different URI'() {
		given:
		def request = new BasicRequest('GET', 'http://qwantz.com/')

		expect:
		!tape.seek(request)
	}

	void 'can seek for a previously recorded interaction'() {
		expect:
		tape.seek(getRequest)
	}

	void 'can read a stored interaction'() {
		when: 'the tape is played'
		def response = tape.play(getRequest)

		then: 'the recorded response data is copied onto the response'
		response.status == plainTextResponse.status
		response.bodyAsText.text == 'O HAI!'
		response.headers == plainTextResponse.headers
	}

	void 'can record post requests with a body'() {
		given: 'a request with some content'
		def request = new BasicRequest('POST', 'http://github.com/')
		request.body = 'q=1'.getBytes('UTF-8')
		request.addHeader(CONTENT_TYPE.toString(), APPLICATION_FORM_URLENCODED.toString())

		when: 'the request and its response are recorded'
		tape.record(request, plainTextResponse)

		then: 'the request body is stored on the tape'
		def interaction = tape.interactions[-1]
		interaction.request.body == request.bodyAsText.text
	}

	void 'a write-only tape cannot be read from'() {
		given: 'the tape is put into write-only mode'
		tape.mode = WRITE_ONLY

		when: 'the tape is played'
		tape.play(getRequest)

		then: 'an exception is thrown'
		def e = thrown(IllegalStateException)
		e.message == 'the tape is not readable'
	}

	void 'a read-only tape cannot be written to'() {
		given: 'the tape is put into read-only mode'
		tape.mode = READ_ONLY

		when: 'the tape is recorded to'
		tape.record(getRequest, plainTextResponse)

		then: 'an exception is thrown'
		def e = thrown(IllegalStateException)
		e.message == 'the tape is not writable'
	}

}

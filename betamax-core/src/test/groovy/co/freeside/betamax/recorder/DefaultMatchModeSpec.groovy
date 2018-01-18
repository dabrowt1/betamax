package co.freeside.betamax.recorder

import co.freeside.betamax.Recorder
import co.freeside.betamax.tape.StorableTape
import co.freeside.betamax.tape.TapeLoader
import co.freeside.betamax.tape.yaml.YamlTape
import spock.lang.Specification
import spock.lang.Unroll
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import static co.freeside.betamax.MatchRule.*

class DefaultMatchModeSpec extends Specification {

  def "default 'default' mode should be [uri, method]"() {
    given:
    def recorder = new Recorder()

    when:
    def defaultDefaultMode = recorder.defaultMatchMode

    then:
    defaultDefaultMode == [uri, method]
  }

  @Unroll
  def "should respect #description default mode"() {
    given:
    def recorder = new Recorder()
    def mockedTape = Mock(YamlTape)

    recorder.metaClass.getTapeLoader = {
      new TapeLoader<YamlTape>() {
        @Override
        YamlTape loadTape(String name) {
          mockedTape
        }

        @Override
        void writeTape(StorableTape tape) {
          throw new NotImplementedException()
        }

        @Override
        File fileFor(String tapeName) {
          return null
        }
      }
    }

    when:
    recorder.insertTape("test", [match: providedMatchMode])

    then:
    1 * mockedTape.setProperty('matchRules', expectedMatchMode)

    where:
    description | providedMatchMode   | expectedMatchMode
    "default"   | null                | [uri, method]
    "@default"  | [defaultMatchValue] | [uri, method]
    "custom"    | [body]              | [body]
  }
}

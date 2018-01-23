/*
 * Copyright 2011 Rob Fletcher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.freeside.betamax

import co.freeside.betamax.message.Request
import co.freeside.betamax.tape.StorableTape
import co.freeside.betamax.tape.Tape
import co.freeside.betamax.tape.TapeLoader
import co.freeside.betamax.tape.yaml.YamlTapeLoader
import co.freeside.betamax.util.PropertiesCategory
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

import java.util.logging.Logger

import static TapeMode.READ_WRITE
import static co.freeside.betamax.MatchRule.*
import static java.util.Collections.EMPTY_MAP

/**
 * This is the main interface to the Betamax proxy. It allows control of Betamax configuration and inserting and
 * ejecting `Tape` instances. The class can also be used as a _JUnit @Rule_ allowing tests annotated with `@Betamax` to
 * run with the Betamax HTTP proxy in the background.
 */
class Recorder implements TestRule {

  public static final String DEFAULT_TAPE_ROOT = 'src/test/resources/betamax/tapes'

  protected final log = Logger.getLogger(getClass().name)

  Recorder() {
    def configFile = Recorder.getResource('/BetamaxConfig.groovy')
    def propertiesFile = Recorder.getResource('/betamax.properties')
    if (configFile) {
      configureFrom new ConfigSlurper().parse(configFile)
    } else if (propertiesFile) {
      def properties = new Properties()
      propertiesFile.withReader { reader ->
        properties.load(reader)
      }
      configureFrom properties
    } else {
      configureWithDefaults()
    }
  }

  Recorder(Properties properties) {
    configureFrom properties
  }

  Recorder(ConfigObject config) {
    configureFrom config
  }

  /**
   * The base directory where tape files are stored.
   */
  File tapeRoot = new File(DEFAULT_TAPE_ROOT)

  /**
   * The default mode for an inserted tape.
   */
  TapeMode defaultMode = READ_WRITE

  /**
   * The default match mode for tapes.
   */
  Comparator<Request>[] defaultMatchMode = [uri, method]

  /**
   * Hosts that are ignored by the proxy. Any connections made will be allowed to proceed normally and not be
   * intercepted.
   */
  Collection<String> ignoreHosts = []

  /**
   * If set to true all connections to localhost addresses are ignored.
   * This is equivalent to setting `ignoreHosts` to `['localhost', '127.0.0.1', InetAddress.localHost.hostName,
   * InetAddress.localHost.hostAddress]`.
   */
  boolean ignoreLocalhost = false

  private StorableTape tape

  void start(String tapeName, Map arguments = [:]) {
    insertTape(tapeName, arguments)
  }

  void stop() {
    ejectTape()
  }

  /**
   * Inserts a tape either creating a new one or loading an existing file from `tapeRoot`.
   * @param name the name of the _tape_.
   * @param arguments customize the behaviour of the tape.
   */
  void insertTape(String name, Map arguments = [:]) {
    tape = tapeLoader.loadTape(name)
    tape.mode = arguments.mode ?: defaultMode
    def matchRules = arguments.match
    if (matchRules == null || matchRules == [defaultMatchValue]) {
      matchRules = defaultMatchMode
    }
    tape.matchRules = matchRules
    tape
  }

  /**
   * Gets the current active _tape_.
   * @return the active _tape_.
   */
  Tape getTape() {
    tape
  }

  /**
   * 'Ejects' the current _tape_, writing its content to file. If the proxy is active after calling this method it
   * will no longer record or play back any HTTP traffic until another tape is inserted.
   */
  void ejectTape() {
    if (tape) {
      tapeLoader.writeTape(tape)
      tape = null
    }
  }

  /**
   * Runs the supplied closure after starting the Betamax proxy and inserting a _tape_. After the closure completes
   * the _tape_ is ejected and the proxy stopped.
   * @param name the name of the _tape_.
   * @param closure the closure to execute.
   * @return the return value of the closure.
   */
  def withTape(String name, Closure closure) {
    withTape(name, EMPTY_MAP, closure)
  }

  /**
   * Runs the supplied closure after starting the Betamax proxy and inserting a _tape_. After the closure completes
   * the _tape_ is ejected and the proxy stopped.
   * @param tapeName the name of the _tape_.
   * @param arguments arguments that affect the operation of the proxy.
   * @param closure the closure to execute.
   * @return the return value of the closure.
   */
  def withTape(String tapeName, Map arguments, Closure closure) {
    try {
      start(tapeName, arguments)
      closure()
    } finally {
      stop()
    }
  }

  @Override
  Statement apply(Statement statement, Description description) {
    def annotation = description.getAnnotation(Betamax)
    if (annotation) {
      log.fine "found @Betamax annotation on '$description.displayName'"
      new Statement() {
        void evaluate() {
          withTape(annotation.tape(), [mode: annotation.mode(), match: annotation.match()]) {
            statement.evaluate()
          }
        }
      }
    } else {
      log.fine "no @Betamax annotation on '$description.displayName'"
      statement
    }
  }

  protected void configureFrom(Properties properties) {
    use(PropertiesCategory) {
      tapeRoot = new File(properties.getProperty('betamax.tapeRoot', DEFAULT_TAPE_ROOT))
      defaultMode = properties.getEnum('betamax.defaultMode', READ_WRITE)
      ignoreHosts = properties.getProperty('betamax.ignoreHosts')?.tokenize(',') ?: []
      ignoreLocalhost = properties.getBoolean('betamax.ignoreLocalhost')
    }
  }

  protected void configureFrom(ConfigObject config) {
    tapeRoot = config.betamax.tapeRoot ?: new File(DEFAULT_TAPE_ROOT)
    defaultMode = config.betamax.defaultMode ?: READ_WRITE
    ignoreHosts = config.betamax.ignoreHosts ?: []
    ignoreLocalhost = config.betamax.ignoreLocalhost
  }

  protected void configureWithDefaults() {
    tapeRoot = new File(DEFAULT_TAPE_ROOT)
    defaultMode = READ_WRITE
    ignoreHosts = []
    ignoreLocalhost = false
  }

  /**
   * Not just a property as `tapeRoot` gets changed during constructor.
   */
  protected TapeLoader getTapeLoader() {
    new YamlTapeLoader(tapeRoot)
  }
}

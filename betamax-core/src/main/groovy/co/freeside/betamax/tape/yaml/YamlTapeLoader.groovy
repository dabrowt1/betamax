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

package co.freeside.betamax.tape.yaml

import java.text.Normalizer
import java.util.logging.Logger
import co.freeside.betamax.tape.*

class YamlTapeLoader implements TapeLoader<YamlTape> {

	public static final String FILE_CHARSET = 'UTF-8'

	final File tapeRoot

	private static final log = Logger.getLogger(YamlTapeLoader.name)

	YamlTapeLoader(File tapeRoot) {
		this.tapeRoot = tapeRoot
	}

	YamlTape loadTape(String name) {
		def file = fileFor(name)
		if (file.isFile()) {
			def tape = file.withReader(FILE_CHARSET) { reader ->
				YamlTape.readFrom(reader)
			}
			log.info "loaded tape with ${tape.size()} recorded interactions from file $file.name..."
			tape
		} else {
			new YamlTape(name: name)
		}
	}

	void writeTape(StorableTape tape) {
		def file = fileFor(tape.name)
		file.parentFile.mkdirs()
		if (tape.isDirty()) {
			file.withWriter(FILE_CHARSET) { writer ->
				log.info "writing tape $tape to file $file.name..."
				tape.writeTo(writer)
			}
		}
	}

	File fileFor(String tapeName) {
		def normalizedName = Normalizer.normalize(tapeName, Normalizer.Form.NFD)
				.replaceAll(/\p{InCombiningDiacriticalMarks}+/, '')
				.replaceAll(/[^\w\d]+/, '_')
				.replaceFirst(/^_/, '')
				.replaceFirst(/_$/, '')
		new File(tapeRoot, "${normalizedName}.yaml")
	}
}

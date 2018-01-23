package co.freeside.betamax.util.servlet

import javax.servlet.ReadListener
import javax.servlet.ServletInputStream

class MockServletInputStream extends ServletInputStream {

	private final InputStream delegate

	MockServletInputStream(InputStream delegate) {
		this.delegate = delegate
	}

	int read() {
		delegate.read()
	}

	@Override
	void close() {
		super.close()
		delegate.close()
	}

	@Override
	synchronized void reset() {
		super.reset()
		delegate.reset()
	}

	// 2018
	@Override
	boolean isReady() {
		// delegate.available() > 0
		throw new UnsupportedOperationException()
	}

	@Override
	boolean isFinished() {
		throw new UnsupportedOperationException()
	}

	@Override
	void setReadListener(ReadListener listener) {
		throw new UnsupportedOperationException()
	}
}

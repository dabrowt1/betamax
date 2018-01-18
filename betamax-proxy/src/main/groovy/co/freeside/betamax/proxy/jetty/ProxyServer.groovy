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

package co.freeside.betamax.proxy.jetty

import co.freeside.betamax.HttpInterceptor
import co.freeside.betamax.ProxyRecorder
import co.freeside.betamax.handler.DefaultHandlerChain
import co.freeside.betamax.util.Network
import co.freeside.betamax.util.ProxyOverrider
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.impl.conn.ProxySelectorRoutePlanner
import org.apache.http.params.HttpConnectionParams

import static ProxyRecorder.DEFAULT_PROXY_PORT

class ProxyServer extends SimpleServer implements HttpInterceptor {

  private final ProxyRecorder recorder
  private final ProxyOverrider proxyOverrider = new ProxyOverrider()

  ProxyServer(ProxyRecorder recorder) {
    super(DEFAULT_PROXY_PORT)
    this.recorder = recorder
  }

  void start() {
    port = recorder.proxyPort

    def handler = new BetamaxProxy()
    handler << new DefaultHandlerChain(recorder, newHttpClient())

    def connectHandler = new CustomConnectHandler(handler, port + 1)

    super.start(connectHandler)

    overrideProxySettings()
  }

  @Override
  void stop() {
    restoreOriginalProxySettings()
    super.stop()
  }

  private HttpClient newHttpClient() {
    def connectionManager = new PoolingClientConnectionManager()
    def httpClient = new DefaultHttpClient(connectionManager)
    httpClient.routePlanner = new ProxySelectorRoutePlanner(
        httpClient.connectionManager.schemeRegistry,
        proxyOverrider.originalProxySelector
    )
    HttpConnectionParams.setConnectionTimeout(httpClient.params, recorder.proxyTimeout)
    HttpConnectionParams.setSoTimeout(httpClient.params, recorder.proxyTimeout)
    httpClient
  }

  private void overrideProxySettings() {
    def proxyHost = InetAddress.localHost.hostAddress
    def nonProxyHosts = recorder.ignoreHosts as Set
    if (recorder.ignoreLocalhost) {
      nonProxyHosts.addAll(Network.localAddresses)
    }
    proxyOverrider.activate proxyHost, port, nonProxyHosts
  }

  private void restoreOriginalProxySettings() {
    proxyOverrider.deactivateAll()
  }

}


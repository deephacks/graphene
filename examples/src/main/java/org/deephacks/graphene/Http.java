/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.graphene;

import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Generic http client.
 */
public class Http {
  public static final int BAD_REQUEST = 400;
  public static final String CONTENT_TYPE = "Content-Type";
  public final static String APPLICATION_JSON = "application/json";
  private final String address;

  public Http(String host, int port) {
    this.address = "http://" + host + ":" + port;
  }

  public String get(String path) {
    return request("GET", path);
  }

  public String post(String path) {
    return request("POST", path);
  }

  private String request(String method, String path) {
    HttpURLConnection conn = null;
    try {
      URL url = new URL(address + path);
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod(method);
      conn.setRequestProperty(CONTENT_TYPE, APPLICATION_JSON);
      conn.setDoOutput(true);
      try (OutputStream os = conn.getOutputStream()) {
        os.flush();
      }
      if (conn.getResponseCode() >= BAD_REQUEST) {
        String body = CharStreams.toString(new InputStreamReader(conn.getErrorStream()));

        throw new RuntimeException("HTTP error code " + conn.getResponseCode() + ". "
                + conn.getResponseMessage() + ". " + body);
      }
      return CharStreams.toString(new InputStreamReader(conn.getInputStream()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }
}

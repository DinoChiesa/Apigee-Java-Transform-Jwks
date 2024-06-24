// Copyright © 2024 Google, LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// All rights reserved.

package com.google.apigee.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpFetch {

  private HttpFetch(String[] args) {}

  private static byte[] readAll(InputStream is) {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      int nRead;
      byte[] data = new byte[1024];
      while ((nRead = is.read(data, 0, data.length)) != -1) {
        os.write(data, 0, nRead);
      }
      os.flush();
      byte[] b = os.toByteArray();
      return b;
    } catch (Exception ex1) {
      return null;
    }
  }

  public static String fetch(final String uri)
      throws IOException, InterruptedException, MalformedURLException {
    System.out.printf("fetch [%s]...\n", uri);

    URL url = new URL(uri);

    // URL connection channel.
    HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();

    // Specify the header and verb
    urlConn.setRequestProperty("Accept", "*/*");
    urlConn.setRequestMethod("GET");

    // Let the run-time system (RTS) know that we want to read the response.
    urlConn.setDoInput(true);

    // Let the RTS know that we want to send something
    urlConn.setDoOutput(false);

    // No caching, we want the real thing.
    urlConn.setUseCaches(false);
    urlConn.connect();

    // Get response data.
    byte[] b = readAll(urlConn.getInputStream());
    String str = new String(b, StandardCharsets.UTF_8);

    return str;
  }
}

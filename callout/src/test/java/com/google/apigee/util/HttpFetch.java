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

/* Requires Java11 for the java.net.http.HttpClient */

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpFetch {

  // public HttpFetch (String[] args) {}
  public static String fetch(String uri)
      throws URISyntaxException, IOException, InterruptedException {
    System.out.printf("fetch [%s]...\n", uri);
    HttpRequest request =
        HttpRequest.newBuilder().header("X-Our-Header-1", "value1").uri(new URI(uri)).GET().build();
    HttpClient client = HttpClient.newHttpClient();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    HttpHeaders headers = response.headers();
    System.out.printf("response headers:\n%s\n", headers.toString());
    String body = response.body();

    System.out.printf("\n\n=>\n%s\n", body);
    return body;
  }
}

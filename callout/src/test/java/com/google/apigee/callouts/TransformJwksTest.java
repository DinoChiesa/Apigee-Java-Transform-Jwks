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

package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionResult;
import com.google.apigee.json.JavaxJson;
import com.google.apigee.util.HttpFetch;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TransformJwksTest extends CalloutTestBase {

  private static final String jwk_from_RFC7517 =
      "{\n"
          + "    \"kty\":\"RSA\",\n"
          + "    \"use\":\"sig\",\n"
          + "    \"kid\":\"1b94c\",\n"
          + "    \"x5c\":[\"MIIDQjCCAiqgAwIBAgIGATz/FuLiMA0GCSqGSIb3DQEBBQUAMGIxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJDTzEPMA0GA1UEBxMGRGVudmVyMRwwGgYDVQQKExNQaW5nIElkZW50aXR5IENvcnAuMRcwFQYDVQQDEw5CcmlhbiBDYW1wYmVsbDAeFw0xMzAyMjEyMzI5MTVaFw0xODA4MTQyMjI5MTVaMGIxCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJDTzEPMA0GA1UEBxMGRGVudmVyMRwwGgYDVQQKExNQaW5nIElkZW50aXR5IENvcnAuMRcwFQYDVQQDEw5CcmlhbiBDYW1wYmVsbDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAL64zn8/QnHYMeZ0LncoXaEde1fiLm1jHjmQsF/449IYALM9if6amFtPDy2yvz3YlRij66s5gyLCyO7ANuVRJx1NbgizcAblIgjtdf/u3WG7K+IiZhtELto/A7Fck9Ws6SQvzRvOE8uSirYbgmj6He4iO8NCyvaK0jIQRMMGQwsU1quGmFgHIXPLfnpnfajr1rVTAwtgV5LEZ4Iel+W1GC8ugMhyr4/p1MtcIM42EA8BzE6ZQqC7VPqPvEjZ2dbZkaBhPbiZAS3YeYBRDWm1p1OZtWamT3cEvqqPpnjL1XyW+oyVVkaZdklLQp2Btgt9qr21m42f4wTw+Xrp6rCKNb0CAwEAATANBgkqhkiG9w0BAQUFAAOCAQEAh8zGlfSlcI0o3rYDPBB07aXNswb4ECNIKG0CETTUxmXl9KUL+9gGlqCz5iWLOgWsnrcKcY0vXPG9J1r9AqBNTqNgHq2G03X09266X5CpOe1zFo+Owb1zxtp3PehFdfQJ610CDLEaS9V9Rqp17hCyybEpOGVwe8fnk+fbEL2Bo3UPGrpsHzUoaGpDftmWssZkhpBJKVMJyf/RuP2SmmaIzmnw9JiSlYhzo4tpzd5rFXhjRbg4zW9C+2qok+2+qDM1iJ684gPHMIY8aLWrdgQTxkumGmTqgawR+N5MDtdPTEQ0XfIBc2cJEUyMTY5MPvACWpkA6SdS4xSvdXK3IVfOWA==\"]\n"
          + "}\n";

  @Test
  public void noSource() throws Exception {
    String method = "noSource() ";
    String expectedError = "source variable resolves to null";
    msgCtxt.setVariable("message-content", "hi there");

    Map<String, String> props = new HashMap<String, String>();
    props.put("debug", "true");
    // props.put("source", "message.content");

    TransformJwks callout = new TransformJwks(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("jwks_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    // System.out.printf("expected error: %s\n", errorOutput);
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("jwks_stacktrace");
    Assert.assertNull(stacktrace, method + "stacktrace");
  }

  @Test
  public void emptySource() throws Exception {
    String method = "emptySource() ";
    String expectedError = "source variable resolves to null";
    msgCtxt.setVariable("message-content", "hi there");

    Map<String, String> props = new HashMap<String, String>();
    props.put("debug", "true");
    props.put("source", "not-message.content"); // not a valid variable

    TransformJwks callout = new TransformJwks(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("jwks_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    // System.out.printf("expected error: %s\n", errorOutput);
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("jwks_stacktrace");
    Assert.assertNull(stacktrace, method + "stacktrace");
  }

  private String fetch(String url) {
    try {
      return HttpFetch.fetch(url);
    } catch (Exception exc1) {
      exc1.printStackTrace();
      Assert.fail();
    }
    return null;
  }

  @Test
  public void noX5c_noop() throws Exception {
    String method = "noX5c_noop() ";
    msgCtxt.setVariable("message.content", fetch("https://www.googleapis.com/oauth2/v3/certs"));

    Map<String, String> props = new HashMap<String, String>();
    props.put("debug", "true");
    props.put("source", "message.content");

    TransformJwks callout = new TransformJwks(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("jwks_error");
    Assert.assertNull(errorOutput, "errorOutput");
    Object outputContent = msgCtxt.getVariable("message.content");
    Assert.assertNotNull(outputContent);
  }

  @Test
  public void success1() throws Exception {
    String method = "success1() ";
    msgCtxt.setVariable("message.content", "{ \"keys\" : [ " + jwk_from_RFC7517 + "  ]}");

    String expectedN =
        "vrjOfz9Ccdgx5nQudyhdoR17V-IubWMeOZCwX_jj0hgAsz2J_pqYW08PLbK_PdiVGKPrqzmDIsLI7sA25VEnHU1uCLNwBuUiCO11_-7dYbsr4iJmG0Qu2j8DsVyT1azpJC_NG84Ty5KKthuCaPod7iI7w0LK9orSMhBEwwZDCxTWq4aYWAchc8t-emd9qOvWtVMDC2BXksRngh6X5bUYLy6AyHKvj-nUy1wgzjYQDwHMTplCoLtU-o-8SNnZ1tmRoGE9uJkBLdh5gFENabWnU5m1ZqZPdwS-qo-meMvVfJb6jJVWRpl2SUtCnYG2C32qvbWbjZ_jBPD5eunqsIo1vQ";
    String expectedE = "AQAB";

    Map<String, String> props = new HashMap<String, String>();
    props.put("debug", "true");
    props.put("source", "message.content");

    TransformJwks callout = new TransformJwks(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("jwks_error");
    Assert.assertNull(errorOutput, "errorOutput");
    Object outputContent = msgCtxt.getVariable("message.content");
    Assert.assertNotNull(outputContent);

    Map<String, Object> jwksjson = JavaxJson.fromJson((String) outputContent, Map.class);
    List<Object> keylist = (List<Object>) jwksjson.get("keys");
    Assert.assertNotNull(keylist);
    Map<String, Object> jwk0 = (Map<String, Object>) keylist.get(0);
    Assert.assertNotNull(jwk0);
    String actualN = (String) jwk0.get("n");
    Assert.assertNotNull(actualN);
    byte[] nbytes = Base64.getUrlDecoder().decode(actualN);
    Assert.assertNotNull(nbytes);
    Assert.assertEquals(nbytes.length, 256);
    Assert.assertEquals(actualN, expectedN);
  }
}

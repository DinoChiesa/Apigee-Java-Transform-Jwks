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
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class JwkToPemTest extends CalloutTestBase {

  public static final String jwk1 =
      "{\"kty\":\"RSA\",\"e\":\"AQAB\",\"kid\":\"b3eeac92\",\"n\":\"goToHtkVP3pxYH7s68B2d2uSTBpAm7v_4amWXxVd3UvNVtLHa8CpGRazcAyZedxyYiU9soTvobi3kFA30_IzBkZdHf5tGOO8EeJOWoCEWIGlZ79oZDMypa8KUZ0woOGdUt2Y7MIn4LJ-QgO1wOuKNvzCjGwUc1TDskAQ0pvAMH8So_NlCMzVWjwFc67upzsZQ1GmRbr-0WfDh-PZI-jzTWBHgedEX3q4JMFy0sJG1cznIXXrhTY6-1Yn3OHtfYI1oKNlZ0J3OKeCnFE_s2D4jOSpyfEeGNB8JvMEjoXdqJggNpS5M9qs3pmdR2Hekc4-Rvt-fI8xyZhHn2KMxBCiUMNBx1XfziARYcTPCBg3M2CZsRT1A8qsI4pL1yNHEUI3-9uMrmdbD9db3E3Y6shAsifeKMSvfTBPAnHltDHOIMjZyoc7FYRLnG_JllZdBFDUtK9axT1g-HWcGblSD9hn-dvuOoiS26CPSZzbE9Duy50pwq0SFkF37vaN4ZtTEdrwnjA1LBa5-TyoUyE7RKpDE5fZ2dpfYnQaav3pzYfB_4157g-t3ZVECtILWcL7baMewFZcXtFrxBvSiNtSnAAb_rFSIiUS6gni_bnBaHTE4T6OEK5eAzFqhXa3QDujkFD70wB73wPqgHOmd6Z0k0sXeqNzo6ntcYH13L8snY6xWz0\"}";

  @Test
  public void noSource() throws Exception {
    String method = "noSource() ";
    String expectedError = "source not specified";
    msgCtxt.setVariable("message-content", "hi there");

    Map<String, String> props = new HashMap<String, String>();
    props.put("debug", "true");
    // props.put("source", "message.content");

    JwkToPem callout = new JwkToPem(props);

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

    JwkToPem callout = new JwkToPem(props);

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
  public void success1() throws Exception {
    String method = "success1() ";
    msgCtxt.setVariable("jwk1", jwk1);

    String expectedPublicKey =
        "-----BEGIN PUBLIC KEY-----\n"
            + "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAgoToHtkVP3pxYH7s68B2\n"
            + "d2uSTBpAm7v/4amWXxVd3UvNVtLHa8CpGRazcAyZedxyYiU9soTvobi3kFA30/Iz\n"
            + "BkZdHf5tGOO8EeJOWoCEWIGlZ79oZDMypa8KUZ0woOGdUt2Y7MIn4LJ+QgO1wOuK\n"
            + "NvzCjGwUc1TDskAQ0pvAMH8So/NlCMzVWjwFc67upzsZQ1GmRbr+0WfDh+PZI+jz\n"
            + "TWBHgedEX3q4JMFy0sJG1cznIXXrhTY6+1Yn3OHtfYI1oKNlZ0J3OKeCnFE/s2D4\n"
            + "jOSpyfEeGNB8JvMEjoXdqJggNpS5M9qs3pmdR2Hekc4+Rvt+fI8xyZhHn2KMxBCi\n"
            + "UMNBx1XfziARYcTPCBg3M2CZsRT1A8qsI4pL1yNHEUI3+9uMrmdbD9db3E3Y6shA\n"
            + "sifeKMSvfTBPAnHltDHOIMjZyoc7FYRLnG/JllZdBFDUtK9axT1g+HWcGblSD9hn\n"
            + "+dvuOoiS26CPSZzbE9Duy50pwq0SFkF37vaN4ZtTEdrwnjA1LBa5+TyoUyE7RKpD\n"
            + "E5fZ2dpfYnQaav3pzYfB/4157g+t3ZVECtILWcL7baMewFZcXtFrxBvSiNtSnAAb\n"
            + "/rFSIiUS6gni/bnBaHTE4T6OEK5eAzFqhXa3QDujkFD70wB73wPqgHOmd6Z0k0sX\n"
            + "eqNzo6ntcYH13L8snY6xWz0CAwEAAQ==\n"
            + "-----END PUBLIC KEY-----\n";

    Map<String, String> props = new HashMap<String, String>();
    props.put("debug", "true");
    props.put("source", "jwk1");

    JwkToPem callout = new JwkToPem(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("jwks_error");
    Assert.assertNull(errorOutput, "errorOutput");
    Object pemContent = msgCtxt.getVariable("jwks_pem");
    Assert.assertNotNull(pemContent);
    Assert.assertEquals(pemContent, expectedPublicKey);
  }
}

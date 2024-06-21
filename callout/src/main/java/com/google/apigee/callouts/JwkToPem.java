// Copyright © 2023-2024 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import com.google.apigee.json.JavaxJson;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JwkToPem extends CalloutBase implements Execution {
  public JwkToPem(Map properties) {
    super(properties);
  }

  protected String getDestination(MessageContext msgCtxt) throws IllegalStateException {
    // Retrieve a value from a named property, as a string.
    String value = (String) this.properties.get("destination");
    if (value != null) value = value.trim();
    if (value == null || value.equals("")) {
      return null;
    }
    value = resolveVariableReferences(value, msgCtxt);
    if (value == null || value.equals("")) {
      return null;
    }
    return value;
  }

  protected static String transform(String jwkContent) throws Exception {
    Map<String, Object> jwk = JavaxJson.fromJson(jwkContent, Map.class);
    String modulus_b64 = (String) jwk.get("n");
    String exponent_b64 = (String) jwk.get("e");

    System.out.printf("n: %s\n", modulus_b64);
    System.out.printf("e: %s\n", exponent_b64);

    byte[] nbytes = Base64.getUrlDecoder().decode(modulus_b64);
    byte[] ebytes = Base64.getUrlDecoder().decode(exponent_b64); // probably AQAB

    BigInteger modulus = new BigInteger(1, nbytes);
    BigInteger publicExponent = new BigInteger(1, ebytes);

    PublicKey publicKey =
        KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, publicExponent));

    byte[] publicKeyBytes = publicKey.getEncoded();

    String base64Encoded = Base64.getEncoder().encodeToString(publicKeyBytes);
    Pattern p = Pattern.compile(".{1,64}");
    Matcher m = p.matcher(base64Encoded);
    String pem =
        "-----BEGIN PUBLIC KEY-----\n" + m.replaceAll("$0\n") + "-----END PUBLIC KEY-----\n";
    return pem;
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      String sourceVariable = getSource(msgCtxt);
      if (sourceVariable == null) {
        throw new IllegalStateException("source not specified");
      }
      String destinationVariable = getDestination(msgCtxt);
      if (destinationVariable == null) {
        destinationVariable = "jwks_pem";
      }

      Object source = msgCtxt.getVariable(sourceVariable);
      if (source == null) {
        throw new IllegalStateException("source variable resolves to null");
      }
      String jwk = (String) source;
      if (jwk == null) {
        throw new IllegalStateException("empty jwk content");
      }

      String pem = transform(jwk);
      msgCtxt.setVariable(destinationVariable, pem);

      return ExecutionResult.SUCCESS;
    } catch (IllegalStateException exc1) {
      setExceptionVariables(exc1, msgCtxt);
      return ExecutionResult.ABORT;
    } catch (Exception e) {
      if (getDebug()) {
        String stacktrace = getStackTraceAsString(e);
        msgCtxt.setVariable(varName("stacktrace"), stacktrace);
      }
      setExceptionVariables(e, msgCtxt);
      return ExecutionResult.ABORT;
    }
  }
}

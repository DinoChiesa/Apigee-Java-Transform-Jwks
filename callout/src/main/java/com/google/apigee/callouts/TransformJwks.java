// TransformJwks.java
//
// This is the source code for a callout for Apigee, which accepts as input
// a JWKS payload, and for each JWK that holds only x5c and no n or e, transforms it
// into a JWK that includes n and e. This works for RSA keys only.
//
// This might be necessary if the JWKS provider omits the n and e parameters.
//
// ------------------------------------------------------------------
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
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import com.google.apigee.json.JavaxJson;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class TransformJwks extends CalloutBase implements Execution {
  public TransformJwks(Map properties) {
    super(properties);
  }

  public static X509Certificate x5cToCert(String x5c) throws Exception {
    byte[] der = Base64.getDecoder().decode(x5c);
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(der);
    try {
      CertificateFactory certFactory = CertificateFactory.getInstance("X.509", "BC");
      Certificate certificate = certFactory.generateCertificate(byteArrayInputStream);
      return (X509Certificate) certificate;
    } catch (CertificateException e) {
      throw new Exception("Unable to convert x5c value to X509Certificate: " + e, e);
    }
  }

  private static byte[] maybeTrimLeadingZero(byte[] array) {
    if (array[0] == 0) {
      byte[] tmp = new byte[array.length - 1];
      System.arraycopy(array, 1, tmp, 0, tmp.length);
      array = tmp;
    }
    return array;
  }

  protected static String encode(BigInteger value) {
    byte[] bytes = value.toByteArray();
    bytes = maybeTrimLeadingZero(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  protected static String transform(String jwksContent) throws Exception {
    Map<String, Object> jwksjson = JavaxJson.fromJson(jwksContent, Map.class);
    List<Object> keylist = (List<Object>) jwksjson.get("keys");
    for (Object item : keylist) {
      Map<String, Object> jwk = (Map<String, Object>) item;
      String kty = (String) jwk.get("kty");
      if ("RSA".equals(kty)) {
        List<String> certificateChain = (List<String>) jwk.get("x5c");
        if (certificateChain != null && certificateChain.size() > 0) {
          // extract Modulus and Exponent
          String x5c = certificateChain.get(0);
          RSAPublicKey publicKey = (RSAPublicKey) x5cToCert(x5c).getPublicKey();
          jwk.put("n", encode(publicKey.getModulus()));
          jwk.put("e", encode(publicKey.getPublicExponent()));
        }
      }
    }
    // re-serialize
    return JavaxJson.toJson(jwksjson);
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      String sourceVariable = getSource(msgCtxt);
      if (sourceVariable == null) {
        sourceVariable = "message.content";
      }

      Object source = msgCtxt.getVariable(sourceVariable);
      if (source == null) {
        throw new IllegalStateException("source variable resolves to null");
      }
      String jwksContent =
          (source instanceof Message) ? ((Message) source).getContent() : (String) source;
      if (jwksContent == null || jwksContent.trim().equals("")) {
        throw new IllegalStateException("empty jwks content");
      }
      String transformedJwks = transform(jwksContent);
      if (source instanceof Message) {
        ((Message) source).setContent(transformedJwks);
      } else {
        msgCtxt.setVariable(sourceVariable, transformedJwks);
      }
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

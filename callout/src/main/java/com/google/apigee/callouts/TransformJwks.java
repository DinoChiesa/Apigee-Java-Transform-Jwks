// TransformJwks.java
//
// This is the source code for a callout for Apigee, which accepts as input
// a JWKS payload, and for each JWK that holds only x5c and no n or e, transforms it
// into a JWK that includes n and e. This works for RSA keys only.
//
// This might be necessary if the JWKS provider omits the n and e parameters.
//
// ------------------------------------------------------------------
// Copyright © 2023 Google LLC.
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransformJwks implements Execution {
  private static final Pattern variableReferencePattern =
      Pattern.compile("(.*?)\\{([^\\{\\} :][^\\{\\} ]*?)\\}(.*?)");
  private static final Pattern commonErrorPattern = Pattern.compile("^(.+?)[:;] (.+)$");
  private Map<String, String> properties; // read-only

  public TransformJwks(Map properties) {
    this.properties = genericizeMap(properties);
  }

  public static Map<String, String> genericizeMap(Map properties) {
    // convert an untyped Map to a generic map
    Map<String, String> m = new HashMap<String, String>();
    Iterator iterator = properties.keySet().iterator();
    while (iterator.hasNext()) {
      Object key = iterator.next();
      Object value = properties.get(key);
      if ((key instanceof String) && (value instanceof String)) {
        m.put((String) key, (String) value);
      }
    }
    return Collections.unmodifiableMap(m);
  }

  protected String varName(String s) {
    return "jwks_" + s;
  }

  protected String getSource(MessageContext msgCtxt) throws IllegalStateException {
    // Retrieve a value from a named property, as a string.
    String value = (String) this.properties.get("source");
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

  protected boolean getDebug() {
    String wantDebug = (String) this.properties.get("debug");
    boolean debug = (wantDebug != null) && Boolean.parseBoolean(wantDebug);
    return debug;
  }

  /*
   *
   * If a property holds one or more segments wrapped with begin and end
   * curlies, eg, {apiproxy.name}, then "resolve" the value by de-referencing
   * the context variable whose name appears between the curlies.
   **/
  protected String resolveVariableReferences(String spec, MessageContext msgCtxt) {
    if (spec == null || spec.equals("")) return spec;
    Matcher matcher = variableReferencePattern.matcher(spec);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(sb, "");
      sb.append(matcher.group(1));
      String ref = matcher.group(2);
      String[] parts = ref.split(":", 2);
      Object v = msgCtxt.getVariable(parts[0]);
      if (v != null) {
        sb.append((String) v);
      } else if (parts.length > 1) {
        sb.append(parts[1]);
      }
      sb.append(matcher.group(3));
    }
    matcher.appendTail(sb);
    return sb.toString();
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

  protected static String encode(BigInteger value) {
    byte[] bytes = value.toByteArray();
    return Base64.getEncoder().encodeToString(bytes);
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

  protected static String getStackTraceAsString(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    return sw.toString();
  }

  protected void setExceptionVariables(Exception exc1, MessageContext msgCtxt) {
    String error = exc1.toString().replaceAll("\n", " ");
    msgCtxt.setVariable(varName("exception"), error);
    Matcher matcher = commonErrorPattern.matcher(error);
    if (matcher.matches()) {
      msgCtxt.setVariable(varName("error"), matcher.group(2));
    } else {
      msgCtxt.setVariable(varName("error"), error);
    }
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      String sourceVariable = getSource(msgCtxt);
      if (sourceVariable == null) {
        sourceVariable = "message.content";
      }

      Object source = msgCtxt.getVariable(sourceVariable);
      String jwksContent =
          (source instanceof Message) ? ((Message) source).getContent() : (String) source;

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

# Apigee custom policy to Transform JWKS

This directory contains the Java source code and pom.xml file required to
compile a Java callout for Apigee. The callout performs one simple job: for a
given JWKS payload, for each JWK with `kty` = "RSA", that includes `x5c` but no
`n` or `e` parameters, it transforms that JWK into one that includes `n` & `e`.

## Why

The VerifyJWT policy in Apigee can select an RSA key from a JWKS if the JWK has
an `n` and a `e` property.  If the JWK has only a `x5c` property, the policy
cannot select it.  This Callout transforms the JWKS with only `x5c` properties
into something that VerifyJWT can use.


## Using this Callout

You do not need to build this project in order to use the callout. To use it,
follow these instructions.

1. If you edit proxy bundles offline, copy the jar file, available in
   `target/apigee-callout-remove-variable-20230526.jar`, as well as the
   dependencies that appear in the `target/lib` directory, to your
   `apiproxy/resources/java` directory.  If you don't edit proxy bundles
   offline, upload the jar file into the API Proxy via the Apigee API Proxy
   Editor .

2. include an XML file for the Java callout policy in your
   apiproxy/resources/policies directory. It should look
   like this:
   ```xml
   <JavaCallout name='Java-Transform-JWKS>
     <Properties>
       <Property name="source">message.content</Property>
     </Properties>
     <ClassName>com.google.apigee.callouts.JwksTransformer</ClassName>
     <ResourceURL>java://apigee-callout-remove-variable-20230526.jar</ResourceURL>
   </JavaCallout>
   ```

   * The `source` value should resolve to a message, or to a string. If you omit it,
     it defaults to `message.content`.

3. Attach this policy to the flow. Usually you will want to attach it to the Response flow.

4. use the Google Cloud Console UI, or a tool like
   [importAndDeploy.js](https://github.com/DinoChiesa/apigee-edge-js-examples/blob/master/importAndDeploy.js)
   or similar to import the proxy into an Apigee organization, and then deploy the
   proxy.  Eg,

   ```
   TOKEN=`gcloud auth print-access-token`
   node ./importAndDeploy -v --token $TOKEN --apigeex -o $ORG -e $ENV  -d bundle
   ```

5. Start a trace in Apigee, then use a client to generate and send http requests
   to the proxy. Eg,

   ```
   curl -i https://my-endpoint.net/transform-jwks-demo/bare
   curl -i https://my-endpoint.net/transform-jwks-demo/transformed
   ```

   In the trace window, you will see that for the 2nd call, the JWKS content
   gets transformed by the callout.


## Building

You do not need to build this project in order to use the callout. If you wish to build it, follow these instructions.

1. unpack (if you can read this, you've already done that).

2. configure the build on your machine by loading the Apigee jars into your local cache
   ```
   ./buildsetup.sh
   ```

   This will satisfy the 2 build-time dependencies,
     - Apigee Edge expressions v1.0
     - Apigee Edge message-flow v1.0

   If you want to download them manually, you get them here:
   https://github.com/apigee/api-platform-samples/tree/master/doc-samples/java-cookbook/lib

2. Build with maven.
   ```
   mvn clean package
   ```

   The result will be a jar you can use in your Apigee proxies.


## Bugs

There are no unit tests for this project.

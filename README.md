# Apigee custom policy to Transform JWKS

This directory contains the Java source code and pom.xml file required to
compile a Java callout for Apigee. There are two callout classes:

- TransformJwks - for a given JWKS payload, for each JWK with `kty` = "RSA",
  that includes `x5c` but no `n` or `e` parameters, it transforms that JWK into
  one that includes `n` & `e`.

- JwkToPem - Converts a single JWK formatted key with `n` and `e` parameters and
  `kty` = "RSA" into a PEM-formatted public key.

Because this is built with Java11, it won't work on Apigee OPDK at this time. 

## Why

Some systems need keys in different formats.  For example, the VerifyJWT policy in Apigee can select an RSA key from a JWKS if the JWK has
an `n` and a `e` property.  If the JWK has only a `x5c` property, the policy
cannot select it.  This Callout transforms the JWKS with only `x5c` properties
into something that VerifyJWT can use.

## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.



## Using this Callout

You do not need to build this project in order to use the callout. To use it,
follow these instructions.

1. If you edit proxy bundles offline, copy the jar file, available in
   `target/apigee-callout-transform-jwks-20240621..jar`, as well as the
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
     ....
     </Properties>
     <ClassName>com.google.apigee.callouts.JwksTransformer</ClassName>
     <ResourceURL>java://apigee-callout-transform-jwks-20240621.jar</ResourceURL>
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



## Example: Convert from x5c

Convert from JWKS with `x5c` to JWKS with `n` and `e`.

```xml
<JavaCallout name='Java-Transform-JWKS>
  <Properties>
    <Property name="source">message.content</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.JwksTransformer</ClassName>
  <ResourceURL>java://apigee-callout-remove-variable-20240621.jar</ResourceURL>
</JavaCallout>
```

The output is placed into the same variable named as `source`.


## Example: Convert a JWK into PEM format

Convert a single JWK from a format that contains `n` and `e`, into a public key in PEM format.

```xml
<JavaCallout name='Java-Transform-JWKS>
  <Properties>
    <Property name="source">jwk1</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.JwkToPem</ClassName>
  <ResourceURL>java://apigee-callout-transform-jwks-20240621.jar</ResourceURL>
</JavaCallout>
```

The output is placed into a variable named `jwks_pem`.  It will look like this:

```
-----BEGIN PUBLIC KEY-----
MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAgoToHtkVP3pxYH7s68B2
d2uSTBpAm7v/4amWXxVd3UvNVtLHa8CpGRazcAyZedxyYiU9soTvobi3kFA30/Iz
BkZdHf5tGOO8EeJOWoCEWIGlZ79oZDMypa8KUZ0woOGdUt2Y7MIn4LJ+QgO1wOuK
NvzCjGwUc1TDskAQ0pvAMH8So/NlCMzVWjwFc67upzsZQ1GmRbr+0WfDh+PZI+jz
TWBHgedEX3q4JMFy0sJG1cznIXXrhTY6+1Yn3OHtfYI1oKNlZ0J3OKeCnFE/s2D4
jOSpyfEeGNB8JvMEjoXdqJggNpS5M9qs3pmdR2Hekc4+Rvt+fI8xyZhHn2KMxBCi
UMNBx1XfziARYcTPCBg3M2CZsRT1A8qsI4pL1yNHEUI3+9uMrmdbD9db3E3Y6shA
sifeKMSvfTBPAnHltDHOIMjZyoc7FYRLnG/JllZdBFDUtK9axT1g+HWcGblSD9hn
+dvuOoiS26CPSZzbE9Duy50pwq0SFkF37vaN4ZtTEdrwnjA1LBa5+TyoUyE7RKpD
E5fZ2dpfYnQaav3pzYfB/4157g+t3ZVECtILWcL7baMewFZcXtFrxBvSiNtSnAAb
/rFSIiUS6gni/bnBaHTE4T6OEK5eAzFqhXa3QDujkFD70wB73wPqgHOmd6Z0k0sX
eqNzo6ntcYH13L8snY6xWz0CAwEAAQ==
-----END PUBLIC KEY-----
```


## Building

You do not need to build this project in order to use the callout. If you wish to build it, follow these instructions.


You need maven v3.9.0 or later, and JDK 11.

1. unpack (if you can read this, you've already done that).

2. Build with maven.
   ```
   mvn clean package
   ```

   The result will be a jar you can use in your Apigee proxies.


## License

This material is Copyright 2023-2024, Google LLC.
and is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.

This code is open source.


## Support

This callout is open-source software, and is not a supported part of Apigee.  If
you need assistance, you can try inquiring on [the Google Cloud Community forum
dedicated to Apigee](https://goo.gle/apigee-community) There is no service-level
guarantee for responses to inquiries posted to that site.


## Bugs

???

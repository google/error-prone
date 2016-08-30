---
title: InsecureCipherMode
summary: Cipher.getInstance() is invoked using either the default settings or ECB mode
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
This error is triggered when a Cipher instance is created using either the
default settings or the notoriously insecure ECB mode. In particular, Java's
default `Cipher.getInstance(AES)` returns a cipher object that operates in ECB
mode. Dynamically constructed transformation strings are also flagged, as they
may conceal an instance of ECB mode. The problem with ECB mode is that
encrypting the same block of plaintext always yields the same block of
ciphertext. Hence, repetitions in the plaintext translate into repetitions in
the ciphertext, which can be readily used to conduct cryptanalysis. The use of
IES-based cipher algorithms also raises an error, as all currently available
implementations use ECB mode under the hood.


----------

### Positive examples
__InsecureCipherModePositiveCases.java__

{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.testdata;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

/**
 * @author avenet@google.com (Arnaud J. Venet)
 */
public class InsecureCipherModePositiveCases {
  static Cipher defaultAesCipher;
  static {
    try {
      // BUG: Diagnostic contains: the mode and padding must be explicitly specified
      defaultAesCipher = Cipher.getInstance("AES");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Cipher defaultRsaCipher;
  static {
    try {
      // BUG: Diagnostic contains: the mode and padding must be explicitly specified
      defaultRsaCipher = Cipher.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static final String AES_STRING = "AES";
  static Cipher defaultAesCipherWithConstantString;
  static {
    try {
      // BUG: Diagnostic contains: the mode and padding must be explicitly specified
      defaultAesCipherWithConstantString = Cipher.getInstance(AES_STRING);
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Cipher explicitAesCipher;
  static {
    try {
      // BUG: Diagnostic contains: ECB mode must not be used
      explicitAesCipher = Cipher.getInstance("AES/ECB/NoPadding");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Cipher explicitDesCipher;
  static {
    try {
      // BUG: Diagnostic contains: ECB mode must not be used
      explicitDesCipher = Cipher.getInstance("DES/ECB/NoPadding");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Cipher explicitDesCipherWithProvider;
  static {
    try {
      // BUG: Diagnostic contains: ECB mode must not be used
      explicitDesCipherWithProvider = Cipher.getInstance("DES/ECB/NoPadding", "My Provider");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchProviderException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static String transformation;
  static {
    try {
      transformation = "DES/CBC/NoPadding";
      // BUG: Diagnostic contains: the transformation is not a compile-time constant
      Cipher cipher = Cipher.getInstance(transformation);
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static void transformationAsParameter(String transformation) {
    try {
      // BUG: Diagnostic contains: the transformation is not a compile-time constant
      Cipher cipher = Cipher.getInstance(transformation);
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  class CipherWrapper {
    Cipher cipher;

    // Make sure that the checker is enabled inside constructors.
    public CipherWrapper() {
      try {
        // BUG: Diagnostic contains: the mode and padding must be explicitly specified
        cipher = Cipher.getInstance("AES");
      } catch (NoSuchAlgorithmException e) {
        // We don't handle any exception as this code is not meant to be executed.
      } catch (NoSuchPaddingException e) {
        // We don't handle any exception as this code is not meant to be executed.
      }
    }
  }

  static Cipher complexCipher1;
  static {
    try {
      String algorithm = "AES";
      // BUG: Diagnostic contains: the transformation is not a compile-time constant
      complexCipher1 = Cipher.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Cipher complexCipher2;
  static {
    try {
      String transformation = "AES";
      transformation += "/ECB";
      transformation += "/NoPadding";
      // BUG: Diagnostic contains: the transformation is not a compile-time constant
      complexCipher2 = Cipher.getInstance(transformation);
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Cipher IesCipher;

  static {
    try {
      // BUG: Diagnostic contains: the mode and padding must be explicitly specified
      IesCipher = Cipher.getInstance("ECIES");
      // BUG: Diagnostic contains: IES
      IesCipher = Cipher.getInstance("ECIES/DHAES/NoPadding");
      // BUG: Diagnostic contains: IES
      IesCipher = Cipher.getInstance("ECIESWITHAES/NONE/PKCS5Padding");
      // BUG: Diagnostic contains: IES
      IesCipher = Cipher.getInstance("DHIESWITHAES/DHAES/PKCS7Padding");
      // BUG: Diagnostic contains: IES
      IesCipher = Cipher.getInstance("ECIESWITHDESEDE/NONE/NOPADDING");
      // BUG: Diagnostic contains: IES
      IesCipher = Cipher.getInstance("DHIESWITHDESEDE/DHAES/PKCS5PADDING");
      // BUG: Diagnostic contains: IES
      IesCipher = Cipher.getInstance("ECIESWITHAES/CBC/PKCS7PADDING");
      // BUG: Diagnostic contains: IES
      IesCipher = Cipher.getInstance("ECIESWITHAES-CBC/NONE/PKCS5PADDING");
      // BUG: Diagnostic contains: IES
      IesCipher = Cipher.getInstance("ECIESwithDESEDE-CBC/DHAES/NOPADDING");

    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }
}
{% endhighlight %}

### Negative examples
__InsecureCipherModeNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.testdata;


import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

/**
 * @author avenet@google.com (Arnaud J. Venet)
 */
public class InsecureCipherModeNegativeCases {
  static Cipher aesCipher;
  static {
    // We don't handle any exception as this code is not meant to be executed.
    try {
      aesCipher = Cipher.getInstance("AES/CBC/NoPadding");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static final String AES_STRING = "AES/CBC/NoPadding";
  static Cipher aesCipherWithConstantString;
  static {
    try {
      aesCipherWithConstantString = Cipher.getInstance(AES_STRING);
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Cipher aesCipherWithProvider;
  static {
    try {
      aesCipherWithProvider = Cipher.getInstance("AES/CBC/NoPadding", "My Provider");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchProviderException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Cipher arc4CipherConscrypt;
  static {
    try {
      arc4CipherConscrypt = Cipher.getInstance("ARC4", "Conscrypt");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchProviderException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Cipher rc4CipherJsch;
  static {
    try {
      rc4CipherJsch = Cipher.getInstance("RC4", "JSch");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchProviderException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Cipher arcfourCipherSunJce;
  static {
    try {
      arcfourCipherSunJce = Cipher.getInstance("ARCFOUR/ECB/NoPadding");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Cipher desCipher;
  static {
    try {
      desCipher = Cipher.getInstance("DES/CBC/NoPadding");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Cipher rsaCipher;
  static {
    try {
      rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Cipher aesWrapCipher;
  static {
    try {
      aesWrapCipher = Cipher.getInstance("AESWrap/ECB/NoPadding");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    } catch (NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

}
{% endhighlight %}


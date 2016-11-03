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


import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
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

  public void ellipticCurveDiffieHellman() {
    KeyFactory keyFactory;
    KeyAgreement keyAgreement;
    KeyPairGenerator keyPairGenerator;
    final String ecdh = "ECDH";
    try {
      keyFactory = KeyFactory.getInstance(ecdh);
      keyAgreement = KeyAgreement.getInstance("ECDH");
      keyPairGenerator = KeyPairGenerator.getInstance("EC" + "DH");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

}

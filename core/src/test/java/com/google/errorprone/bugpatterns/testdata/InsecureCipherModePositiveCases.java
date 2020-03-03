/*
 * Copyright 2015 The Error Prone Authors.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Signature;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;

/** @author avenet@google.com (Arnaud J. Venet) */
public class InsecureCipherModePositiveCases {
  static Cipher defaultAesCipher;

  static {
    try {
      // BUG: Diagnostic contains: Use of these APIs is considered insecure
      defaultAesCipher = Cipher.getInstance("AES");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static final String AES_STRING = "AES";
  static Cipher defaultAesCipherWithConstantString;

  static {
    try {
      // BUG: Diagnostic contains: Use of these APIs is considered insecure
      defaultAesCipherWithConstantString = Cipher.getInstance(AES_STRING);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Cipher explicitDesCipherWithProvider;

  static {
    try {
      // BUG: Diagnostic contains: Use of these APIs is considered insecure
      explicitDesCipherWithProvider = Cipher.getInstance("DES/ECB/NoPadding", "My Provider");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | NoSuchProviderException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  class CipherWrapper {
    Cipher cipher;

    // Make sure that the checker is enabled inside constructors.
    public CipherWrapper() {
      try {
        // BUG: Diagnostic contains: Use of these APIs is considered insecure
        cipher = Cipher.getInstance("AES");
      } catch (NoSuchAlgorithmException e) {
        // We don't handle any exception as this code is not meant to be executed.
      } catch (NoSuchPaddingException e) {
        // We don't handle any exception as this code is not meant to be executed.
      }
    }
  }

  static Mac mac;

  static {
    try {
      // BUG: Diagnostic contains: Use of these APIs is considered insecure
      mac = Mac.getInstance("HmacSHA1");

    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static MessageDigest messageDigest;

  static {
    try {
      // BUG: Diagnostic contains: Use of these APIs is considered insecure
      messageDigest = MessageDigest.getInstance("SHA");

    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static Signature sig;

  static {
    try {
      // BUG: Diagnostic contains: Use of these APIs is considered insecure
      sig = Signature.getInstance("SHA1withRSA");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static KeyAgreement keyAgreement;

  static {
    try {
      // BUG: Diagnostic contains: Use of these APIs is considered insecure
      keyAgreement = KeyAgreement.getInstance("ECDH");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static KeyPairGenerator keyPairGenerator;

  static {
    try {
      // BUG: Diagnostic contains: Use of these APIs is considered insecure
      keyPairGenerator = KeyPairGenerator.getInstance("EC" + "DH");
    } catch (NoSuchAlgorithmException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static CipherInputStream inputStream;

  static {
    try {
      // BUG: Diagnostic contains: Use of these APIs is considered insecure
      Cipher cipher = Cipher.getInstance("AES");
      File file = new File("ok");
      FileInputStream in = new FileInputStream(file);
      // BUG: Diagnostic contains: Use of these APIs is considered insecure
      inputStream = new CipherInputStream(in, cipher);
    } catch (FileNotFoundException | NoSuchAlgorithmException | NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }

  static CipherOutputStream outputStream;

  static {
    try {
      // BUG: Diagnostic contains: Use of these APIs is considered insecure
      Cipher cipher = Cipher.getInstance("AES");
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      // BUG: Diagnostic contains: Use of these APIs is considered insecure
      outputStream = new CipherOutputStream(os, cipher);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      // We don't handle any exception as this code is not meant to be executed.
    }
  }


}

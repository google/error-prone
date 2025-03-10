/*
 * Copyright 2020 The Error Prone Authors.
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

import com.google.errorprone.bugpatterns.BanSerializableReadTest;
import java.io.DataInput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;

/**
 * {@link BanSerializableReadTest}
 *
 * @author tshadwell@google.com (Thomas Shadwell)
 */
public class BanSerializableReadNegativeCases implements Serializable, Externalizable {
  public final String hi = "hi";
  public Integer testField;

  // mostly a smoke test
  public static void noCrimesHere() {
    System.out.println(new BanSerializableReadNegativeCases().hi);
  }

  /**
   * The checker has a special allowlist that allows classes to define methods called readObject --
   * Java accepts these as an override to the default serialization behaviour. While we want to
   * allow such methods to be defined, we don't want to allow these methods to be called.
   *
   * <p>this version has the checks suppressed
   *
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @SuppressWarnings("BanSerializableRead")
  public static final void directCall() throws IOException, ClassNotFoundException {
    PipedInputStream in = new PipedInputStream();
    PipedOutputStream out = new PipedOutputStream(in);

    ObjectOutputStream serializer = new ObjectOutputStream(out);
    ObjectInputStream deserializer = new ObjectInputStream(in);

    BanSerializableReadPositiveCases self = new BanSerializableReadPositiveCases();
    self.readObject(deserializer);
  }

  /**
   * Says 'hi' by piping the string value unsafely through Object I/O stream. This one has the check
   * suppressed, though
   *
   * @throws IOException
   * @throws ClassNotFoundException
   */
  @SuppressWarnings("BanSerializableRead")
  public static void sayHi() throws IOException, ClassNotFoundException {
    PipedInputStream in = new PipedInputStream();
    PipedOutputStream out = new PipedOutputStream(in);

    ObjectOutputStream serializer = new ObjectOutputStream(out);
    ObjectInputStream deserializer = new ObjectInputStream(in);

    serializer.writeObject(new BanSerializableReadPositiveCases());
    serializer.close();

    BanSerializableReadPositiveCases crime =
        (BanSerializableReadPositiveCases) deserializer.readObject();
    System.out.println(crime.hi);
  }

  // These test the more esoteric annotations

  // code has gone through a security review
  @SuppressWarnings("BanSerializableRead")
  public static void directCall2() throws IOException, ClassNotFoundException {
    PipedInputStream in = new PipedInputStream();
    PipedOutputStream out = new PipedOutputStream(in);

    ObjectOutputStream serializer = new ObjectOutputStream(out);
    ObjectInputStream deserializer = new ObjectInputStream(in);

    BanSerializableReadPositiveCases self = new BanSerializableReadPositiveCases();
    self.readObject(deserializer);
  }

  // code is well-tested legacy
  @SuppressWarnings("BanSerializableRead")
  public static final void directCall3() throws IOException, ClassNotFoundException {
    PipedInputStream in = new PipedInputStream();
    PipedOutputStream out = new PipedOutputStream(in);

    ObjectOutputStream serializer = new ObjectOutputStream(out);
    ObjectInputStream deserializer = new ObjectInputStream(in);

    BanSerializableReadPositiveCases self = new BanSerializableReadPositiveCases();
    self.readObject(deserializer);
  }

  // code is for Android
  @SuppressWarnings("BanSerializableRead")
  public static final void directCall4() throws IOException, ClassNotFoundException {
    PipedInputStream in = new PipedInputStream();
    PipedOutputStream out = new PipedOutputStream(in);

    ObjectOutputStream serializer = new ObjectOutputStream(out);
    ObjectInputStream deserializer = new ObjectInputStream(in);

    BanSerializableReadPositiveCases self = new BanSerializableReadPositiveCases();
    self.readObject(deserializer);
  }

  // calls to readObject should themselves be excluded in a readObject method
  void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    BanSerializableReadNegativeCases c = new BanSerializableReadNegativeCases();
    c.readObject(ois);
    ois.defaultReadObject();
  }

  private static class SomeSerializable implements Serializable {
    // A readObject method that doesn't actually satisfy Serializable interface.
    public void readObject(DataInput ois) throws IOException, ClassNotFoundException {}
  }

  public static final void callOverloadedReadObject() throws IOException, ClassNotFoundException {
    new SomeSerializable().readObject(null);
  }

  private static class ObjectInputStreamIsExempt extends ObjectInputStream {
    ObjectInputStreamIsExempt() throws IOException, ClassNotFoundException {
      super();
    }

    @Override
    public Object readObjectOverride() throws IOException, ClassNotFoundException {
      // Calling readObjectOverride is banned by the checker; therefore, overrides can
      // call other banned methods without added risk.
      return super.readObject();
    }
  }

  @Override
  public void readExternal(ObjectInput in) {
    try {
      testField = (Integer) in.readObject();
    } catch (IOException | ClassNotFoundException e) {
    }
  }

  @Override
  public void writeExternal(ObjectOutput out) {
    try {
      out.writeObject(testField);
    } catch (IOException e) {
    }
  }
}

/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PrivateSecurityContractProtoAccessTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(PrivateSecurityContractProtoAccess.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "PrivateSecurityContractProtoAccessPositiveCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

import com.google.common.html.types.SafeHtmlProto;
import com.google.protobuf.ByteString;

public class PrivateSecurityContractProtoAccessPositiveCases {
  static SafeHtmlProto safeHtmlProto;

  static {
    safeHtmlProto =
        SafeHtmlProto.newBuilder()
            // BUG: Diagnostic contains: Forbidden access to a private proto field
            .clearPrivateDoNotAccessOrElseSafeHtmlWrappedValue()
            // BUG: Diagnostic contains: Forbidden access to a private proto field
            .setPrivateDoNotAccessOrElseSafeHtmlWrappedValue("foo")
            .build();
  }

  static SafeHtmlProto safeHtmlProtoFromBytes;

  static {
    safeHtmlProtoFromBytes =
        SafeHtmlProto.newBuilder()
            // BUG: Diagnostic contains: Forbidden access to a private proto field
            .setPrivateDoNotAccessOrElseSafeHtmlWrappedValueBytes(ByteString.copyFromUtf8("foo"))
            .build();
  }

  static String readSafeHtmlProto(SafeHtmlProto safeHtmlProto) {
    // BUG: Diagnostic contains: Forbidden access to a private proto field
    if (safeHtmlProto.hasPrivateDoNotAccessOrElseSafeHtmlWrappedValue()) {
      // BUG: Diagnostic contains: Forbidden access to a private proto field
      return safeHtmlProto.getPrivateDoNotAccessOrElseSafeHtmlWrappedValue();
    }
    return "";
  }

  static ByteString readSafeHtmlProtoBytes(SafeHtmlProto safeHtmlProto) {
    // BUG: Diagnostic contains: Forbidden access to a private proto field
    return safeHtmlProto.getPrivateDoNotAccessOrElseSafeHtmlWrappedValueBytes();
  }

  static String readSafeHtmlProtoBuilder(SafeHtmlProto.Builder safeHtmlProto) {
    // BUG: Diagnostic contains: Forbidden access to a private proto field
    if (safeHtmlProto.hasPrivateDoNotAccessOrElseSafeHtmlWrappedValue()) {
      // BUG: Diagnostic contains: Forbidden access to a private proto field
      return safeHtmlProto.getPrivateDoNotAccessOrElseSafeHtmlWrappedValue();
    }
    return "";
  }

  static ByteString readSafeHtmlProtoBuilderBytes(SafeHtmlProto.Builder safeHtmlProto) {
    // BUG: Diagnostic contains: Forbidden access to a private proto field
    return safeHtmlProto.getPrivateDoNotAccessOrElseSafeHtmlWrappedValueBytes();
  }
}
""")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "PrivateSecurityContractProtoAccessNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import com.google.common.html.types.SafeHtml;
            import com.google.common.html.types.SafeHtmlProto;
            import com.google.common.html.types.SafeHtmls;

            public class PrivateSecurityContractProtoAccessNegativeCases {
              static SafeHtmlProto safeHtmlProto;

              static {
                safeHtmlProto = SafeHtmls.toProto(SafeHtml.EMPTY);
              }

              static SafeHtml safeHtml;

              static {
                safeHtml = SafeHtmls.fromProto(safeHtmlProto);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void safeHtmlAccessWithinPackage() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            """
            package com.google.common.html.types;

            import com.google.common.html.types.SafeHtmlProto;

            class Test {
              SafeHtmlProto buildProto() {
                return SafeHtmlProto.newBuilder()
                    .setPrivateDoNotAccessOrElseSafeHtmlWrappedValue("foo")
                    .build();
              }
            }
            """)
        .doTest();
  }
}

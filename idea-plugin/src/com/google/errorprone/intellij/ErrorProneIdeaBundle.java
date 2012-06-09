package com.google.errorprone.intellij;

import com.intellij.CommonBundle;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

public class ErrorProneIdeaBundle {
  private static Reference<ResourceBundle> ourBundle;

  @NonNls
  private static final String BUNDLE = "com.google.errorprone.intellij.ErrorProneIdeaBundle";

  private ErrorProneIdeaBundle() {
  }

  public static String jdkHomeNotFoundMessage(final Sdk jdk) {
    return message("error-prone.error.jdk.home.missing", jdk.getName(), jdk.getHomePath());
  }

  public static String message(@PropertyKey(resourceBundle = BUNDLE)String key, Object... params) {
    return CommonBundle.message(getBundle(), key, params);
  }

  private static ResourceBundle getBundle() {
    ResourceBundle bundle = null;
    if (ourBundle != null) bundle = ourBundle.get();
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE);
      ourBundle = new SoftReference<ResourceBundle>(bundle);
    }
    return bundle;
  }

  public static String compilerName() {
    return message("error-prone.name");
  }
}

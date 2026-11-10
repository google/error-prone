/*
 * Copyright 2026 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.enclosingPackage;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

/// A [BugChecker]; see the associated [BugPattern] annotation for details.
@BugPattern(
    altNames = "PlatformClassShadowing",
    summary = "Top-level class shadows a class on the compilation bootclasspath or platform",
    severity = WARNING)
public final class BootclasspathShadowing extends BugChecker implements ClassTreeMatcher {

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol sym = getSymbol(tree);
    if (sym.getNestingKind() != NestingKind.TOP_LEVEL
        || sym.getSimpleName().contentEquals("package-info")) {
      return NO_MATCH;
    }
    PackageSymbol enclosingPkg = enclosingPackage(sym).orElse(null);
    if (enclosingPkg == null
        || (enclosingPkg.modle != null
            && (enclosingPkg.modle.flags() & Flags.SYSTEM_MODULE) != 0)) {
      return NO_MATCH;
    }
    String binaryName = sym.flatName().toString();
    if (!isPlatformClass(enclosingPkg, binaryName, state)) {
      return NO_MATCH;
    }
    return describeMatch(tree);
  }

  private static boolean isPlatformClass(
      PackageSymbol enclosingPkg, String binaryName, VisitorState state) {
    JavaFileManager fileManager = state.context.get(JavaFileManager.class);
    try {
      if (fileManager.hasLocation(StandardLocation.PLATFORM_CLASS_PATH)
          && fileManager.getJavaFileForInput(
                  StandardLocation.PLATFORM_CLASS_PATH, binaryName, Kind.CLASS)
              != null) {
        return true;
      }
      for (PackageSymbol pkg : state.getSymtab().getPackagesForName(enclosingPkg.fullname)) {
        ModuleSymbol module = pkg.modle;
        if (module == null || (module.flags() & Flags.SYSTEM_MODULE) == 0) {
          continue;
        }
        Location location = module.classLocation;
        if (location == null && fileManager.hasLocation(StandardLocation.SYSTEM_MODULES)) {
          location =
              fileManager.getLocationForModule(
                  StandardLocation.SYSTEM_MODULES, module.name.toString());
        }
        if (location != null
            && fileManager.getJavaFileForInput(location, binaryName, Kind.CLASS) != null) {
          return true;
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return false;
  }
}

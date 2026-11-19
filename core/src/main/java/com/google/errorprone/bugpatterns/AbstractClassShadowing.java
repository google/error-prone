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

import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.enclosingPackage;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.errorprone.ErrorProneFlags;
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
import java.util.EnumSet;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;

/// Base class for [BootclasspathShadowing] and [ClasspathShadowing].
abstract class AbstractClassShadowing extends BugChecker implements ClassTreeMatcher {

  private final StandardLocation flatLocation;
  private final StandardLocation moduleLocation;
  private final boolean systemModules;

  AbstractClassShadowing(
      StandardLocation flatLocation,
      StandardLocation moduleLocation,
      boolean systemModules,
      ErrorProneFlags errorProneFlags) {
    this.flatLocation = flatLocation;
    this.moduleLocation = moduleLocation;
    this.systemModules = systemModules;
  }

  @Override
  public final Description matchClass(ClassTree tree, VisitorState state) {
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
    if (!isShadowedClass(enclosingPkg, binaryName, state)) {
      return NO_MATCH;
    }
    return describeMatch(tree);
  }

  private boolean isShadowedClass(
      PackageSymbol enclosingPkg, String binaryName, VisitorState state) {
    JavaFileManager fileManager = state.context.get(JavaFileManager.class);
    try {
      if (hasClassInLocation(fileManager, flatLocation, enclosingPkg, binaryName)) {
        return true;
      }
      for (PackageSymbol pkg : state.getSymtab().getPackagesForName(enclosingPkg.fullname)) {
        ModuleSymbol module = pkg.modle;
        if (module == null || module.equals(enclosingPkg.modle)) {
          continue;
        }
        boolean isSystemModule = (module.flags() & Flags.SYSTEM_MODULE) != 0;
        if (isSystemModule != systemModules) {
          continue;
        }
        Location location = module.classLocation;
        if (location == null && fileManager.hasLocation(moduleLocation)) {
          location = fileManager.getLocationForModule(moduleLocation, module.name.toString());
        }
        if (location != null
            && !location.isOutputLocation()
            && fileManager.getJavaFileForInput(location, binaryName, Kind.CLASS) != null) {
          return true;
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return false;
  }

  private static boolean hasClassInLocation(
      JavaFileManager fileManager, Location location, PackageSymbol enclosingPkg, String binaryName)
      throws IOException {
    if (!fileManager.hasLocation(location)) {
      return false;
    }
    JavaFileObject classFile = fileManager.getJavaFileForInput(location, binaryName, Kind.CLASS);
    if (classFile == null) {
      return false;
    }
    // Build tools like Maven pass the output directory (e.g. target/classes) in both -d and at the
    // front of -classpath, so on non-clean builds CLASS_PATH may already contain stale .class files
    // from CLASS_OUTPUT. Check CLASS_OUTPUT via getJavaFileForInput + isSameFile (rather than
    // JavacFileManager.contains, which throws ProviderMismatchException across file systems), and
    // if the first hit is in CLASS_OUTPUT, scan the rest of the package on location so shadowed
    // dependency jars behind it are still detected.
    JavaFileObject outputFile =
        fileManager.hasLocation(StandardLocation.CLASS_OUTPUT)
            ? fileManager.getJavaFileForInput(StandardLocation.CLASS_OUTPUT, binaryName, Kind.CLASS)
            : null;
    if (outputFile == null || !fileManager.isSameFile(classFile, outputFile)) {
      return true;
    }
    for (JavaFileObject candidate :
        fileManager.list(
            location,
            enclosingPkg.fullname.toString(),
            EnumSet.of(Kind.CLASS),
            /* recurse= */ false)) {
      if (!fileManager.isSameFile(candidate, outputFile)
          && binaryName.equals(fileManager.inferBinaryName(location, candidate))) {
        return true;
      }
    }
    return false;
  }
}

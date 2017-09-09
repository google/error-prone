/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.toType;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Suppliers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "DefaultCharset",
  category = JDK,
  summary =
      "Implicit use of the platform default charset, which can result in e.g. non-ASCII"
          + " characters being silently replaced with '?' in many environments",
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class DefaultCharset extends BugChecker
    implements MethodInvocationTreeMatcher, NewClassTreeMatcher {

  enum CharsetFix {
    UTF_8_FIX("UTF_8") {
      @Override
      void addImport(SuggestedFix.Builder fix, VisitorState state) {
        fix.addStaticImport("java.nio.charset.StandardCharsets.UTF_8");
      }
    },
    DEFAULT_CHARSET_FIX("Charset.defaultCharset()") {
      @Override
      void addImport(SuggestedFix.Builder fix, VisitorState state) {
        fix.addImport("java.nio.charset.Charset");
      }
    };

    final String replacement;

    CharsetFix(String replacement) {
      this.replacement = replacement;
    }

    String replacement() {
      return replacement;
    }

    abstract void addImport(SuggestedFix.Builder fix, VisitorState state);
  }

  // ignore the constructor that takes FileDescriptor; it's rare and there's no automated fix
  private static final Matcher<ExpressionTree> FILE_WRITER =
      anyOf(
          constructor().forClass(FileWriter.class.getName()).withParameters("java.io.File"),
          constructor()
              .forClass(FileWriter.class.getName())
              .withParameters("java.io.File", "boolean"),
          constructor().forClass(FileWriter.class.getName()).withParameters("java.lang.String"),
          constructor()
              .forClass(FileWriter.class.getName())
              .withParameters("java.lang.String", "boolean"));

  private static final Matcher<Tree> BUFFERED_WRITER =
      toType(ExpressionTree.class, constructor().forClass(BufferedWriter.class.getName()));

  private static final Matcher<ExpressionTree> FILE_READER =
      anyOf(
          constructor().forClass(FileReader.class.getName()).withParameters("java.io.File"),
          constructor().forClass(FileReader.class.getName()).withParameters("java.lang.String"));

  private static final Matcher<Tree> BUFFERED_READER =
      toType(ExpressionTree.class, constructor().forClass(BufferedReader.class.getName()));

  private static final Matcher<ExpressionTree> CTOR =
      anyOf(
          constructor()
              .forClass(String.class.getName())
              .withParameters(ImmutableList.of(Suppliers.arrayOf(Suppliers.BYTE_TYPE))),
          constructor()
              .forClass(String.class.getName())
              .withParameters(
                  ImmutableList.of(
                      Suppliers.arrayOf(Suppliers.BYTE_TYPE),
                      Suppliers.INT_TYPE,
                      Suppliers.INT_TYPE)),
          constructor()
              .forClass(OutputStreamWriter.class.getName())
              .withParameters(ImmutableList.of(Suppliers.typeFromClass(OutputStream.class))),
          constructor()
              .forClass(InputStreamReader.class.getName())
              .withParameters(ImmutableList.of(Suppliers.typeFromClass(InputStream.class))));

  private static final Matcher<ExpressionTree> BYTESTRING_COPY_FROM =
      staticMethod().onClass("com.google.protobuf.ByteString").named("copyFrom");

  private static final Matcher<ExpressionTree> STRING_GET_BYTES =
      instanceMethod().onExactClass(String.class.getName()).withSignature("getBytes()");

  private static final Matcher<ExpressionTree> FILE_NEW_WRITER =
      staticMethod()
          .onClass(com.google.common.io.Files.class.getName())
          .named("newWriter")
          .withParameters("java.lang.String");

  private static final Matcher<ExpressionTree> PRINT_WRITER =
      anyOf(
          constructor().forClass(PrintWriter.class.getName()).withParameters(File.class.getName()),
          constructor()
              .forClass(PrintWriter.class.getName())
              .withParameters(String.class.getName()));

  private static final Matcher<ExpressionTree> PRINT_WRITER_OUTPUTSTREAM =
      anyOf(
          constructor()
              .forClass(PrintWriter.class.getName())
              .withParameters(OutputStream.class.getName()),
          constructor()
              .forClass(PrintWriter.class.getName())
              .withParameters(OutputStream.class.getName(), "boolean"));

  private static final Matcher<ExpressionTree> SCANNER_MATCHER =
      anyOf(
          constructor()
              .forClass(Scanner.class.getName())
              .withParameters(InputStream.class.getName()),
          constructor().forClass(Scanner.class.getName()).withParameters(File.class.getName()),
          constructor().forClass(Scanner.class.getName()).withParameters(Path.class.getName()),
          constructor()
              .forClass(Scanner.class.getName())
              .withParameters(ReadableByteChannel.class.getName()));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (state.isAndroidCompatible()) {
      return NO_MATCH;
    }
    if (STRING_GET_BYTES.matches(tree, state)) {
      Description.Builder description = buildDescription(tree);
      Tree parent = state.getPath().getParentPath().getLeaf();
      if (parent instanceof ExpressionTree
          && BYTESTRING_COPY_FROM.matches((ExpressionTree) parent, state)) {
        byteStringFixes(description, tree, (ExpressionTree) parent, state);
      } else {
        appendCharsets(description, tree, tree.getMethodSelect(), tree.getArguments(), state);
      }
      return description.build();
    }
    if (FILE_NEW_WRITER.matches(tree, state)) {
      Description.Builder description = buildDescription(tree);
      appendCharsets(description, tree, tree.getMethodSelect(), tree.getArguments(), state);
      return description.build();
    }
    return NO_MATCH;
  }

  private static void byteStringFixes(
      Description.Builder description,
      MethodInvocationTree tree,
      ExpressionTree parent,
      VisitorState state) {
    description.addFix(byteStringFix(tree, parent, state, ".copyFromUtf8(", "").build());

    SuggestedFix.Builder builder =
        byteStringFix(
            tree, parent, state, ".copyFrom(", ", " + CharsetFix.DEFAULT_CHARSET_FIX.replacement());
    CharsetFix.DEFAULT_CHARSET_FIX.addImport(builder, state);
    description.addFix(builder.build());
  }

  private static SuggestedFix.Builder byteStringFix(
      MethodInvocationTree tree,
      ExpressionTree parent,
      VisitorState state,
      String prefix,
      String suffix) {
    return SuggestedFix.builder()
        .replace(
            /*startPos=*/ state.getEndPosition(ASTHelpers.getReceiver(parent)),
            /*endPos=*/ ((JCTree) tree).getStartPosition(),
            /*replaceWith=*/ prefix)
        .replace(
            state.getEndPosition(ASTHelpers.getReceiver(tree)), state.getEndPosition(tree), suffix);
  }

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (state.isAndroidCompatible()) {
      return NO_MATCH;
    }
    if (CTOR.matches(tree, state)) {
      Description.Builder description = buildDescription(tree);
      appendCharsets(description, tree, tree.getIdentifier(), tree.getArguments(), state);
      return description.build();
    }
    if (FILE_READER.matches(tree, state)) {
      return handleFileReader(tree, state);
    }
    if (FILE_WRITER.matches(tree, state)) {
      return handleFileWriter(tree, state);
    }
    if (PRINT_WRITER.matches(tree, state)) {
      return handlePrintWriter(tree, state);
    }
    if (PRINT_WRITER_OUTPUTSTREAM.matches(tree, state)) {
      return handlePrintWriterOutputStream(tree, state);
    }
    if (SCANNER_MATCHER.matches(tree, state)) {
      return handleScanner(tree, state);
    }
    return NO_MATCH;
  }

  private Description handleScanner(NewClassTree tree, VisitorState state) {
    Description.Builder description = buildDescription(tree);
    for (CharsetFix charsetFix : CharsetFix.values()) {
      SuggestedFix.Builder fix =
          SuggestedFix.builder()
              .postfixWith(
                  getOnlyElement(tree.getArguments()),
                  String.format(", %s.name()", charsetFix.replacement()));
      charsetFix.addImport(fix, state);
      description.addFix(fix.build());
    }
    return description.build();
  }

  boolean shouldUseGuava(VisitorState state) {
    for (ImportTree importTree : state.getPath().getCompilationUnit().getImports()) {
      Symbol sym = ASTHelpers.getSymbol(importTree.getQualifiedIdentifier());
      if (sym == null) {
        continue;
      }
      if (sym.getQualifiedName().contentEquals("com.google.common.io.Files")) {
        return true;
      }
    }
    return false;
  }

  private Description handleFileReader(NewClassTree tree, VisitorState state) {
    Tree arg = getOnlyElement(tree.getArguments());
    Tree parent = state.getPath().getParentPath().getLeaf();
    Tree toReplace = BUFFERED_READER.matches(parent, state) ? parent : tree;
    Description.Builder description = buildDescription(tree);
    fileReaderFix(description, state, arg, toReplace);
    return description.build();
  }

  private void fileReaderFix(
      Description.Builder description, VisitorState state, Tree arg, Tree toReplace) {
    for (CharsetFix charset : CharsetFix.values()) {
      if (shouldUseGuava(state)) {
        description.addFix(guavaFileReaderFix(state, arg, toReplace, charset));
      } else {
        description.addFix(nioFileReaderFix(state, arg, toReplace, charset));
      }
    }
  }

  private Fix nioFileReaderFix(VisitorState state, Tree arg, Tree toReplace, CharsetFix charset) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    fix.replace(
        toReplace,
        String.format(
            "Files.newBufferedReader(%s, %s)", toPath(state, arg, fix), charset.replacement()));
    fix.addImport("java.nio.file.Files");
    charset.addImport(fix, state);
    variableTypeFix(fix, state, FileReader.class, Reader.class);
    return fix.build();
  }

  private Fix guavaFileReaderFix(
      VisitorState state, Tree fileArg, Tree toReplace, CharsetFix charset) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    fix.replace(
        toReplace,
        String.format(
            "Files.newReader(%s, %s)", toFile(state, fileArg, fix), charset.replacement()));
    fix.addImport("com.google.common.io.Files");
    charset.addImport(fix, state);
    variableTypeFix(fix, state, FileReader.class, Reader.class);
    return fix.build();
  }

  private void variableTypeFix(
      SuggestedFix.Builder fix, VisitorState state, Class<?> original, Class<?> replacement) {
    Tree parent = state.getPath().getParentPath().getLeaf();
    Symbol sym;
    switch (parent.getKind()) {
      case VARIABLE:
        sym = ASTHelpers.getSymbol((VariableTree) parent);
        break;
      case ASSIGNMENT:
        sym = ASTHelpers.getSymbol(((AssignmentTree) parent).getVariable());
        break;
      default:
        return;
    }
    if (!ASTHelpers.isSameType(
        sym.type, state.getTypeFromString(original.getCanonicalName()), state)) {
      return;
    }
    state
        .getPath()
        .getCompilationUnit()
        .accept(
            new TreeScanner<Void, Void>() {
              @Override
              public Void visitVariable(VariableTree node, Void aVoid) {
                if (sym.equals(ASTHelpers.getSymbol(node))) {
                  fix.replace(node.getType(), replacement.getSimpleName())
                      .addImport(replacement.getCanonicalName());
                }
                return null;
              }
            },
            null);
  }

  private Description handleFileWriter(NewClassTree tree, VisitorState state) {
    Iterator<? extends ExpressionTree> it = tree.getArguments().iterator();
    Tree fileArg = it.next();
    Tree appendMode = it.hasNext() ? it.next() : null;
    Tree parent = state.getPath().getParentPath().getLeaf();
    Tree toReplace = BUFFERED_WRITER.matches(parent, state) ? parent : tree;
    Description.Builder description = buildDescription(tree);
    boolean useGuava = shouldUseGuava(state);
    for (CharsetFix charset : CharsetFix.values()) {
      if (appendMode == null && useGuava) {
        description.addFix(guavaFileWriterFix(state, fileArg, toReplace, charset));
      } else {
        description.addFix(
            nioFileWriterFix(state, appendMode, fileArg, toReplace, charset, useGuava));
      }
    }
    return description.build();
  }

  private Fix guavaFileWriterFix(
      VisitorState state, Tree fileArg, Tree toReplace, CharsetFix charset) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    fix.replace(
        toReplace,
        String.format(
            "Files.newWriter(%s, %s)", toFile(state, fileArg, fix), charset.replacement()));
    fix.addImport("com.google.common.io.Files");
    charset.addImport(fix, state);
    variableTypeFix(fix, state, FileWriter.class, Writer.class);
    return fix.build();
  }

  private Fix nioFileWriterFix(
      VisitorState state,
      Tree appendTree,
      Tree fileArg,
      Tree toReplace,
      CharsetFix charset,
      boolean qualify) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    StringBuilder sb = new StringBuilder();
    if (qualify) {
      sb.append("java.nio.file.Files");
    } else {
      sb.append("Files");
      fix.addImport("java.nio.file.Files");
    }
    sb.append(".newBufferedWriter(");
    sb.append(toPath(state, fileArg, fix));
    sb.append(", ").append(charset.replacement());
    charset.addImport(fix, state);
    if (appendTree != null) {
      sb.append(toAppendMode(fix, appendTree, state));
    }
    sb.append(")");
    fix.replace(toReplace, sb.toString());
    variableTypeFix(fix, state, FileWriter.class, Writer.class);
    return fix.build();
  }

  /** Convert a boolean append mode to a StandardOpenOption. */
  private String toAppendMode(SuggestedFix.Builder fix, Tree appendArg, VisitorState state) {
    // recognize constants to try to avoid `true ? CREATE, APPEND : CREATE`
    Boolean value = ASTHelpers.constValue(appendArg, Boolean.class);
    if (value != null) {
      if (value) {
        fix.addStaticImport("java.nio.file.StandardOpenOption.APPEND");
        fix.addStaticImport("java.nio.file.StandardOpenOption.CREATE");
        return ", CREATE, APPEND";
      } else {
        // CREATE is the default
        return "";
      }
    }
    fix.addImport("java.nio.file.StandardOpenOption");
    fix.addStaticImport("java.nio.file.StandardOpenOption.APPEND");
    fix.addStaticImport("java.nio.file.StandardOpenOption.CREATE");
    return String.format(
        ", %s ? new StandardOpenOption[] {CREATE, APPEND} : new StandardOpenOption[] {CREATE}",
        state.getSourceForNode(appendArg));
  }

  /** Converts a {@code String} to a {@code File}. */
  private Object toFile(VisitorState state, Tree fileArg, SuggestedFix.Builder fix) {
    Type type = ASTHelpers.getType(fileArg);
    if (ASTHelpers.isSubtype(type, state.getSymtab().stringType, state)) {
      fix.addImport("java.io.File");
      return String.format("new File(%s)", state.getSourceForNode(fileArg));
    } else if (ASTHelpers.isSubtype(type, state.getTypeFromString("java.io.File"), state)) {
      return state.getSourceForNode(fileArg);
    } else {
      throw new AssertionError("unexpected type: " + type);
    }
  }

  /** Convert a {@code String} or {@code File} argument to a {@code Path}. */
  private String toPath(VisitorState state, Tree fileArg, SuggestedFix.Builder fix) {
    Type type = ASTHelpers.getType(fileArg);
    if (ASTHelpers.isSubtype(type, state.getSymtab().stringType, state)) {
      fix.addImport("java.nio.file.Paths");
      return String.format("Paths.get(%s)", state.getSourceForNode(fileArg));
    } else if (ASTHelpers.isSubtype(type, state.getTypeFromString("java.io.File"), state)) {
      return String.format("%s.toPath()", state.getSourceForNode(fileArg));
    } else {
      throw new AssertionError("unexpected type: " + type);
    }
  }

  private void appendCharsets(
      Description.Builder description,
      Tree tree,
      Tree select,
      List<? extends ExpressionTree> arguments,
      VisitorState state) {
    description.addFix(appendCharset(tree, select, arguments, state, CharsetFix.UTF_8_FIX));
    description.addFix(
        appendCharset(tree, select, arguments, state, CharsetFix.DEFAULT_CHARSET_FIX));
  }

  private Fix appendCharset(
      Tree tree,
      Tree select,
      List<? extends ExpressionTree> arguments,
      VisitorState state,
      CharsetFix charset) {
    SuggestedFix.Builder fix = SuggestedFix.builder();
    if (arguments.isEmpty()) {
      fix.replace(
          state.getEndPosition(select),
          state.getEndPosition(tree),
          String.format("(%s)", charset.replacement()));
    } else {
      fix.postfixWith(Iterables.getLast(arguments), ", " + charset.replacement());
    }
    charset.addImport(fix, state);
    return fix.build();
  }

  private Description handlePrintWriter(NewClassTree tree, VisitorState state) {
    Description.Builder description = buildDescription(tree);
    for (CharsetFix charsetFix : CharsetFix.values()) {
      SuggestedFix.Builder fix =
          SuggestedFix.builder()
              .postfixWith(
                  getOnlyElement(tree.getArguments()),
                  String.format(", %s.name()", charsetFix.replacement()));
      charsetFix.addImport(fix, state);
      description.addFix(fix.build());
    }
    return description.build();
  }

  private Description handlePrintWriterOutputStream(NewClassTree tree, VisitorState state) {
    Tree outputStream = tree.getArguments().get(0);
    Description.Builder description = buildDescription(tree);
    for (CharsetFix charsetFix : CharsetFix.values()) {
      SuggestedFix.Builder fix =
          SuggestedFix.builder()
              .prefixWith(outputStream, "new BufferedWriter(new OutputStreamWriter(")
              .postfixWith(outputStream, String.format(", %s))", charsetFix.replacement()));
      charsetFix.addImport(fix, state);
      fix.addImport("java.io.BufferedWriter");
      fix.addImport("java.io.OutputStreamWriter");
      description.addFix(fix.build());
    }
    return description.build();
  }
}

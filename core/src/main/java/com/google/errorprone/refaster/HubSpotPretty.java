package com.google.errorprone.refaster;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.escape.ArrayBasedCharEscaper;
import com.google.common.escape.CharEscaper;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.util.Convert;

// Taken from our internal BetterPretty class
class HubSpotPretty extends Pretty {
  private static final CharEscaper JAVA_STRING_ESCAPER = JavaStringEscaper.create();
  private final Writer out;

  public HubSpotPretty(Writer out, boolean sourceOutput) {
    super(out, sourceOutput);
    this.out = out;
    this.width = 2;
  }

  @Override
  public final void print(Object s) throws IOException {
    // The super method does weird escaping of values
    out.append(s.toString());
  }

  @Override
  public final void visitLiteral(JCLiteral tree) {
    try {
      switch (tree.typetag) {
        case INT:
        case DOUBLE:
          print(tree.value.toString());
          break;
        case LONG:
          print(tree.value + "L");
          break;
        case FLOAT:
          print(tree.value + "F");
          break;
        case CHAR:
          print(
              "'" +
                  Convert.quote(String.valueOf((char) ((Number) tree.value).intValue())) +
                  "'"
          );
          break;
        case BOOLEAN:
          print(((Number) tree.value).intValue() == 0 ? "false" : "true");
          break;
        case BOT:
          print("null");
          break;
        case CLASS:
          // this should only ever be strings
          print("\"" + JAVA_STRING_ESCAPER.escape(tree.value.toString()) + "\"");
          break;
        default:
          print("\"" + Convert.quote(tree.value.toString()) + "\"");
          break;
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public final void visitLambda(JCLambda tree) {
    try {
      writeLambdaParameters(tree);
      print(" -> ");
      printExpr(tree.body);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public final void visitAnnotation(JCAnnotation tree) {
    try {
      print("@");
      printExpr(tree.annotationType);
      if (!tree.args.isEmpty()) {
        print("(");
        if (tree.args.size() == 1) {
          JCExpression expression = tree.args.get(0);
          if (!(expression instanceof JCAssign)) {
            printExprs(tree.args);
          } else {
            JCAssign assign = (JCAssign) expression;
            if (((JCIdent) assign.lhs).name.contentEquals("value")) {
              printExpr(assign.rhs);
            } else {
              printExprs(tree.args);
            }
          }
        } else {
          printExprs(tree.args);
        }
        print(")");
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void writeLambdaParameters(JCLambda tree) throws IOException {
    if (tree.paramKind == JCLambda.ParameterKind.EXPLICIT) {
      print("(");
      printExprs(tree.params);
      print(")");
    } else if (tree.params.size() == 1) {
      // Special case `foo -> bar`
      print(tree.params.get(0).name);
    } else {
      // Everything else needs parens
      print("(");
      print(
          tree
              .getParameters()
              .stream()
              .map(VariableTree::getName)
              .collect(Collectors.joining(", "))
      );
      print(")");
    }
  }

  // This escaper does not produce octal escape sequences. See:
  // http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#101089
  //  "Octal escapes are provided for compatibility with C, but can express
  //   only Unicode values \u0000 through \u00FF, so Unicode escapes are
  //   usually preferred."
  private static class JavaStringEscaper extends ArrayBasedCharEscaper {

    // From: http://en.wikipedia.org/wiki/ASCII#ASCII_printable_characters
    private static final char PRINTABLE_ASCII_MIN = 0x20; // ' '
    private static final char PRINTABLE_ASCII_MAX = 0x7E; // '~'

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    JavaStringEscaper(Map<Character, String> replacements) {
      super(replacements, PRINTABLE_ASCII_MIN, PRINTABLE_ASCII_MAX);
    }

    @Override
    protected char[] escapeUnsafe(char c) {
      // Escape iso control characters and possibly confusing space characters
      // The printable ascii ranges cover the common cases of ' ' and friends, so they
      // won't be processed by this method
      if (Character.isISOControl(c) || Character.isSpaceChar(c)) {
        return asUnicodeHexEscape(c);
      } else {
        return new char[]{c};
      }
    }

    // Helper for common case of escaping a single char.
    private static char[] asUnicodeHexEscape(char c) {
      // Equivalent to String.format("\\u%04x", (int)c);
      char[] r = new char[6];
      r[0] = '\\';
      r[1] = 'u';
      r[5] = HEX_DIGITS[c & 0xF];
      c >>>= 4;
      r[4] = HEX_DIGITS[c & 0xF];
      c >>>= 4;
      r[3] = HEX_DIGITS[c & 0xF];
      c >>>= 4;
      r[2] = HEX_DIGITS[c & 0xF];
      return r;
    }

    static JavaStringEscaper create() {
      Map<Character, String> javaMap = new HashMap<>();
      javaMap.put('\b', "\\b");
      javaMap.put('\f', "\\f");
      javaMap.put('\n', "\\n");
      javaMap.put('\r', "\\r");
      javaMap.put('\t', "\\t");
      javaMap.put('\"', "\\\"");
      javaMap.put('\\', "\\\\");
      // OMITTED: javaMap.put('\'', "\\'");
      return new JavaStringEscaper(javaMap);
    }
  }
}

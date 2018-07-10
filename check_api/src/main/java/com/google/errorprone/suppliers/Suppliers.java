/*
 * Copyright 2012 The Error Prone Authors.
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

package com.google.errorprone.suppliers;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

/** @author alexeagle@google.com (Alex Eagle) */
public class Suppliers {

  /**
   * Supplies the n'th generic type of the given expression. For example, in {@code Map<A,B> c;} for
   * the expression c and n=1, the result is the type of {@code B}. If there are an insufficient
   * number of type arguments, this method will return the {@code java.lang.Object} type from symbol
   * table.
   *
   * @param expressionSupplier a supplier of the expression which has a generic type
   * @param n the position of the generic argument
   */
  public static Supplier<Type> genericTypeOf(
      final Supplier<ExpressionTree> expressionSupplier, final int n) {
    return new Supplier<Type>() {
      @Override
      public Type get(VisitorState state) {
        JCExpression jcExpression = (JCExpression) expressionSupplier.get(state);
        if (jcExpression.type.getTypeArguments().size() <= n) {
          return state.getSymtab().objectType;
        }
        return jcExpression.type.getTypeArguments().get(n);
      }
    };
  }

  /**
   * Supplies the n'th generic type of the given expression. For example, in {@code Map<A,B> c;} for
   * the type of c and n=1, the result is the type of {@code B}. If there are an insufficient number
   * of type arguments, this method will return the {@code java.lang.Object} type from symbol table.
   *
   * @param typeSupplier a supplier of the expression which has a generic type
   * @param n the position of the generic argument
   */
  public static Supplier<Type> genericTypeOfType(final Supplier<Type> typeSupplier, final int n) {
    return new Supplier<Type>() {
      @Override
      public Type get(VisitorState state) {
        Type type = typeSupplier.get(state);
        if (type.getTypeArguments().size() <= n) {
          return state.getSymtab().objectType;
        }
        return type.getTypeArguments().get(n);
      }
    };
  }

  /**
   * Supplies the expression which gives the instance of an object that will receive the method
   * call. For example, in {@code a.getB().getC()} if the visitor is currently visiting the {@code
   * getC()} method invocation, then this supplier gives the expression {@code a.getB()}.
   */
  public static Supplier<Type> receiverType() {
    return new Supplier<Type>() {
      @Override
      public Type get(VisitorState state) {
        MethodInvocationTree methodInvocation = (MethodInvocationTree) state.getPath().getLeaf();
        return ASTHelpers.getReceiverType(methodInvocation.getMethodSelect());
      }
    };
  }

  /**
   * Supplies the expression which gives the instance of an object that will receive the method
   * call. For example, in {@code a.getB().getC()} if the visitor is currently visiting the {@code
   * getC()} method invocation, then this supplier gives the expression {@code a.getB()}.
   */
  public static Supplier<ExpressionTree> receiverInstance() {
    return new Supplier<ExpressionTree>() {
      @Override
      public ExpressionTree get(VisitorState state) {
        MethodInvocationTree method = (MethodInvocationTree) state.getPath().getLeaf();
        return ((JCFieldAccess) method.getMethodSelect()).getExpression();
      }
    };
  }

  /**
   * Given the string representation of a type, supplies the corresponding type.
   *
   * @param typeString a string representation of a type, e.g., "java.util.List"
   */
  public static Supplier<Type> typeFromString(final String typeString) {
    requireNonNull(typeString);
    return new Supplier<Type>() {
      @Override
      public Type get(VisitorState state) {
        return state.getTypeFromString(typeString);
      }
    };
  }

  /** Given the class representation of a type, supplies the corresponding type. */
  public static Supplier<Type> typeFromClass(Class<?> inputClass) {
    return typeFromString(inputClass.getName());
  }

  public static final Supplier<Type> JAVA_LANG_VOID_TYPE =
      new Supplier<Type>() {
        @Override
        public Type get(VisitorState state) {
          return state.getTypeFromString("java.lang.Void");
        }
      };

  public static final Supplier<Type> VOID_TYPE =
      new Supplier<Type>() {
        @Override
        public Type get(VisitorState state) {
          return state.getSymtab().voidType;
        }
      };

  public static final Supplier<Type> JAVA_LANG_BOOLEAN_TYPE =
      new Supplier<Type>() {
        @Override
        public Type get(VisitorState state) {
          return state.getTypeFromString("java.lang.Boolean");
        }
      };

  public static final Supplier<Type> STRING_TYPE =
      new Supplier<Type>() {
        @Override
        public Type get(VisitorState state) {
          return state.getSymtab().stringType;
        }
      };

  public static final Supplier<Type> BOOLEAN_TYPE =
      new Supplier<Type>() {
        @Override
        public Type get(VisitorState state) {
          return state.getSymtab().booleanType;
        }
      };

  public static final Supplier<Type> BYTE_TYPE =
      new Supplier<Type>() {
        @Override
        public Type get(VisitorState state) {
          return state.getSymtab().byteType;
        }
      };

  public static final Supplier<Type> INT_TYPE =
      new Supplier<Type>() {
        @Override
        public Type get(VisitorState state) {
          return state.getSymtab().intType;
        }
      };

  public static final Supplier<Type> CHAR_TYPE =
      new Supplier<Type>() {
        @Override
        public Type get(VisitorState state) {
          return state.getSymtab().charType;
        }
      };

  public static final Supplier<Type> OBJECT_TYPE =
      new Supplier<Type>() {
        @Override
        public Type get(VisitorState state) {
          return state.getSymtab().objectType;
        }
      };

  public static final Supplier<Type> EXCEPTION_TYPE =
      new Supplier<Type>() {
        @Override
        public Type get(VisitorState state) {
          return state.getSymtab().exceptionType;
        }
      };

  public static final Supplier<Type> THROWABLE_TYPE =
      new Supplier<Type>() {
        @Override
        public Type get(VisitorState state) {
          return state.getSymtab().throwableType;
        }
      };

  public static final Supplier<Type> ANNOTATION_TYPE =
      new Supplier<Type>() {
        @Override
        public Type get(VisitorState state) {
          return state.getSymtab().annotationType;
        }
      };

  /**
   * Supplies what was given. Useful for adapting to methods that require a supplier.
   *
   * @param toSupply the item to supply
   */
  public static <T> Supplier<T> identitySupplier(final T toSupply) {
    return new Supplier<T>() {
      @Override
      public T get(VisitorState state) {
        return toSupply;
      }
    };
  }

  public static final Supplier<Type> ENCLOSING_CLASS =
      new Supplier<Type>() {
        @Override
        public Type get(VisitorState state) {
          return ((JCTree) state.findEnclosing(ClassTree.class)).type;
        }
      };

  public static Supplier<Type> arrayOf(final Supplier<Type> elementType) {
    return new Supplier<Type>() {
      @Override
      public Type get(VisitorState state) {
        return new Type.ArrayType(elementType.get(state), state.getSymtab().arraysType.tsym);
      }
    };
  }

  public static ImmutableList<Supplier<Type>> fromStrings(Iterable<String> types) {
    return ImmutableList.copyOf(
        Iterables.transform(
            types,
            new Function<String, Supplier<Type>>() {
              @Override
              public Supplier<Type> apply(String input) {
                return Suppliers.typeFromString(input);
              }
            }));
  }
}

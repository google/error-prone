/*
 * Copyright 2019 The Error Prone Authors.
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

package com.google.errorprone.matchers.method;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;

/**
 * The machinery and type definitions necessary to model and compile a single efficient matcher out
 * of a list of {@link com.google.errorprone.matchers.method.MethodMatchers.MethodMatcher}s.
 */
public class MethodInvocationMatcher {

  static final class Context {
    final MethodSymbol sym;
    final ExpressionTree tree;

    Context(MethodSymbol sym, ExpressionTree tree) {
      this.sym = sym;
      this.tree = tree;
    }

    public static Optional<Context> create(ExpressionTree tree) {
      Symbol sym = ASTHelpers.getSymbol(tree);
      if (!(sym instanceof MethodSymbol)) {
        return Optional.empty();
      }
      return Optional.of(new Context((MethodSymbol) sym, tree));
    }
  }

  /**
   * The kinds of things that count as a method invocation.
   *
   * <p><strong>This is part of the low-level API for constructing Rule objects dynamically.
   * Consider using the fluent API from MethodMatcher, and the associated helpers in Matchers, when
   * possible.</strong>
   */
  public enum MethodKind {
    STATIC,
    INSTANCE,
    CONSTRUCTOR
  }

  /**
   * The kinds of properties a matcher can match against.
   *
   * <p><strong>This is part of the low-level API for constructing Rule objects dynamically.
   * Consider using the fluent API from MethodMatcher, and the associated helpers in Matchers, when
   * possible.</strong>
   *
   * <p><em>The order of these enum constants is important</em>: it's the order in which we will
   * search the graph (because we iterate over values() to decide what order to check predicates).
   * Therefore, prefer putting first those properties which are cheap to check, or which will often
   * result in a failure to match (so we can prune the search space).
   */
  public enum TokenType {
    KIND {
      @Override
      MethodKind extract(Context ctx, VisitorState s) {
        return ctx.sym.getKind() == ElementKind.CONSTRUCTOR
            ? MethodKind.CONSTRUCTOR
            : ctx.sym.isStatic() ? MethodKind.STATIC : MethodKind.INSTANCE;
      }
    },
    METHOD_NAME {
      @Override
      String extract(Context ctx, VisitorState s) {
        return ctx.sym.getSimpleName().toString();
      }
    },
    PARAMETER_TYPES {
      @Override
      ImmutableList<String> extract(Context ctx, VisitorState s) {
        return ctx.sym.getParameters().stream()
            .map(param -> param.type.tsym.getQualifiedName().toString())
            .collect(ImmutableList.toImmutableList());
      }
    },
    DEFINED_IN {
      @Override
      String extract(Context ctx, VisitorState s) {
        return ctx.sym.owner.getQualifiedName().toString();
      }
    },
    RECEIVER_TYPE {
      @Override
      String extract(Context ctx, VisitorState s) {
        return ASTHelpers.getReceiverType(ctx.tree).tsym.getQualifiedName().toString();
      }
    },
    RECEIVER_SUPERTYPE {
      @Override
      Type extract(Context ctx, VisitorState s) {
        return ASTHelpers.getReceiverType(ctx.tree).tsym.type;
      }
    };

    abstract Object extract(Context ctx, VisitorState s);
  }

  /**
   * A specific value for a property that a method invocation can have.
   *
   * <p><strong>This is part of the low-level API for constructing Rule objects dynamically.
   * Consider using the fluent API from MethodMatcher, and the associated helpers in Matchers, when
   * possible. Do not create your own implementations of this interface.</strong>
   */
  public interface Token {

    /** The category of properties that this value falls into. */
    TokenType type();

    /**
     * The value to compare with {@link TokenType#extract(Context, VisitorState)} to determine
     * whether this property matches.
     */
    Object comparisonKey();

    /** A token limiting the {@link Kind} of invocation to match. */
    @AutoValue
    abstract class Kind implements Token {
      public abstract MethodKind kind();

      public static Kind create(MethodKind kind) {
        return new AutoValue_MethodInvocationMatcher_Token_Kind(kind);
      }

      @Override
      public MethodKind comparisonKey() {
        return kind();
      }

      @Override
      public TokenType type() {
        return TokenType.KIND;
      }
    }

    /** A token limiting the name of the method being invoked. */
    @AutoValue
    abstract class MethodName implements Token {
      public abstract String methodName();

      @Override
      public Object comparisonKey() {
        return methodName();
      }

      @Override
      public TokenType type() {
        return TokenType.METHOD_NAME;
      }

      public static MethodName create(String methodName) {
        return new AutoValue_MethodInvocationMatcher_Token_MethodName(methodName);
      }
    }

    /** A token limiting the types of the formal parameters of the method being invoked. */
    @AutoValue
    abstract class ParameterTypes implements Token {
      public abstract ImmutableList<String> parameterTypes();

      @Override
      public TokenType type() {
        return TokenType.PARAMETER_TYPES;
      }

      @Override
      public Object comparisonKey() {
        return parameterTypes();
      }

      public static ParameterTypes create(ImmutableList<String> types) {
        return new AutoValue_MethodInvocationMatcher_Token_ParameterTypes(types);
      }
    }

    /** A token specifying the class or interface in which the invoked method was defined. */
    @AutoValue
    abstract class DefinedIn implements Token {
      public abstract String owner();

      public static DefinedIn create(String owner) {
        return new AutoValue_MethodInvocationMatcher_Token_DefinedIn(owner);
      }

      @Override
      public TokenType type() {
        return TokenType.DEFINED_IN;
      }

      @Override
      public Object comparisonKey() {
        return owner();
      }
    }

    /**
     * A token specifying the exact type of the object on which the method is being invoked (or the
     * class in which it is defined, for static methods).
     */
    @AutoValue
    abstract class ReceiverType implements Token {
      public abstract String receiverType();

      public static ReceiverType create(String receiverType) {
        return new AutoValue_MethodInvocationMatcher_Token_ReceiverType(receiverType);
      }

      @Override
      public Object comparisonKey() {
        return receiverType();
      }

      @Override
      public TokenType type() {
        return TokenType.RECEIVER_TYPE;
      }
    }

    /**
     * A token specifying that the class of the object on which the method is being invoked must be
     * a subtype of another type.
     */
    @AutoValue
    abstract class ReceiverSupertype implements Token {
      public abstract String receiverSupertype();

      @Override
      public TokenType type() {
        return TokenType.RECEIVER_SUPERTYPE;
      }

      public static ReceiverSupertype create(String receiverSupertype) {
        return new AutoValue_MethodInvocationMatcher_Token_ReceiverSupertype(receiverSupertype);
      }

      @Override
      public Object comparisonKey() {
        // Can't do a key-based lookup for a supertype, but it's still useful to be able to put
        // these things in a map for iterating them.
        return receiverSupertype();
      }
    }
  }

  /**
   * A rule describing a set of constraints for a method invocation. For each TokenType, a Rule
   * specifies 0 or more Tokens describing what values are allowed for that type.
   *
   * <p><strong>This is part of the low-level API for constructing Rule objects dynamically.
   * Consider using the fluent API from MethodMatcher, and the associated helpers in Matchers, when
   * possible.</strong>
   */
  @AutoValue
  public abstract static class Rule {

    /** Builds a Rule object from a map. */
    public static Rule create(ImmutableMap<TokenType, ? extends Set<Token>> required) {
      return new AutoValue_MethodInvocationMatcher_Rule(required);
    }

    // An absent token means to allow any value for this token type
    public abstract ImmutableMap<TokenType, ? extends Set<Token>> required();
  }

  /** A Node is just a type synonym for Object - it's just a unique pointer. */
  private static class Node {}

  /**
   * A map describing where to look next based on the current token, and a default if none match.
   */
  private static class NodeWithDefault {

    private final Set<Node> states;
    @Nullable final Set<Node> def;
    final SetMultimap<Token, Node> mapping;

    NodeWithDefault(Set<Node> states, Set<Node> def, SetMultimap<Token, Node> mapping) {
      this.states = states;
      this.def = def;
      this.mapping = mapping;
    }
  }

  /** Shared by all compiled graphs, because it has no varying properties. */
  private static final Node ACCEPT = new Node();

  /** Converts a DFA produced by {@link #compile(Iterable)} into a Matcher based on map lookups. */
  private static final class GraphMatcher {
    static Matcher<ExpressionTree> from(
        Map<Set<Node>, NodeWithDefault> mappings, NodeWithDefault root) {
      BiPredicate<Context, VisitorState> pred = traverse(mappings, root);
      return (tree, state) -> {
        Optional<Context> ctx = Context.create(tree);
        // Could be ctx.map(...).orElse(false), but why pay to box the Boolean?
        return ctx.isPresent() && pred.test(ctx.get(), state);
      };
    }

    private static BiPredicate<Context, VisitorState> traverse(
        Map<Set<Node>, NodeWithDefault> mappings, NodeWithDefault root) {
      if (root.states.contains(ACCEPT)) {
        // If there was any path from the root to the accept node, the predicate matched.
        return (ctx, state) -> true;
      }
      SetMultimap<Token, Node> children = root.mapping;
      if (children.isEmpty()) {
        Preconditions.checkArgument(root.def != null, "Found node with no mappings and no default");
        // Since this node is only a default, we don't have to bother checking its token type at
        // all, and can just return the next matcher we "would have" unconditionally delegated to.
        return traverse(mappings, mappings.get(root.def));
      }
      ImmutableSet<TokenType> tokenTypes =
          children.keySet().stream().map(Token::type).collect(ImmutableSet.toImmutableSet());
      Preconditions.checkArgument(
          tokenTypes.size() == 1,
          "Found mismatched token types in node with mappings %s",
          children);

      // We have a valid input. Translate each of its children into a Predicate, and return a new
      // Predicate that delegates appropriately depending on context
      TokenType type = tokenTypes.iterator().next(); // safe since the set is a singleton.
      BiPredicate<Context, VisitorState> defaultBehavior;
      if (root.def == null) {
        defaultBehavior = (ctx, state) -> false;
      } else {
        defaultBehavior = traverse(mappings, mappings.get(root.def));
      }

      Map<Object, BiPredicate<Context, VisitorState>> lookup = new HashMap<>();
      @SuppressWarnings("UnstableApiUsage")
      Set<Map.Entry<Token, Set<Node>>> entries = Multimaps.asMap(children).entrySet();
      // Would be cleaner as a stream collecting into a map, but the cost of that stream operation
      // is non-negligible and DFA compilation needs to be faster.
      for (Map.Entry<Token, Set<Node>> entry : entries) {
        lookup.put(
            entry.getKey().comparisonKey(), traverse(mappings, mappings.get(entry.getValue())));
      }

      switch (type) {
        case RECEIVER_SUPERTYPE:
          return (ctx, state) -> {
            Type receiverType = (Type) TokenType.RECEIVER_SUPERTYPE.extract(ctx, state);
            // Have to iterate here because subclassing can't be checked by lookup.
            for (Map.Entry<Object, BiPredicate<Context, VisitorState>> child : lookup.entrySet()) {
              if (ASTHelpers.isSubtype(
                  receiverType, state.getTypeFromString((String) child.getKey()), state)) {
                return child.getValue().test(ctx, state);
              }
            }
            return defaultBehavior.test(ctx, state);
          };
        default:
          return (ctx, state) -> {
            // All other token types can be checked via a map lookup.
            Object lookupKey = type.extract(ctx, state);
            BiPredicate<Context, VisitorState> child = lookup.get(lookupKey);
            if (child != null) {
              return child.test(ctx, state);
            }
            return defaultBehavior.test(ctx, state);
          };
      }
    }
  }

  /**
   * Constructs a Matcher that matches for method invocations (including constructor invocations)
   * satisfying at least one of the given Rule specifications. For an easy way to create such Rules,
   * see the factories in {@link com.google.errorprone.matchers.Matchers} returning subtypes of
   * {@link com.google.errorprone.matchers.method.MethodMatchers.MethodMatcher}.
   */
  public static Matcher<ExpressionTree> compile(Iterable<Rule> rules) {

    // A set of Rule objects represents a predicate stated in disjunctive normal form, where the
    // atoms are equality tests on parts of a MethodSymbol allowed by TokenType and Token classes.
    //
    // We optimize for frequent evaluation of this predicate by first compiling it into a graph
    // where each node has a single outgoing edge for each relevant token, so that each token
    // comparison is just a lookup in a map of edges, instead of a linear scan over N rules.
    // The final graph will have one root state, and one accept state; if we reach the accept, we
    // say the predicate matches, and if we ever find no matching edge from the current node, we say
    // the predicate fails to match. To construct this graph optimally, we start with an NFA with
    // one path from the root to accept per rule; and then apply a modified version of the power-set
    // construction to reduce it to an equivalent DFA.

    Table<Node, Optional<Token>, Node> nfa = HashBasedTable.create();
    ImmutableSet.Builder<Node> rootsBuilder = ImmutableSet.builder();
    for (Rule rule : rules) {
      ImmutableMap<TokenType, ? extends Set<Token>> required = rule.required();
      int numTokens = required.size();
      if (numTokens == 0) {
        // Forget this whole graph business if one of the alternatives is "anything". This isn't
        // just an optimization: it simplifies building the graph if we know no rules are empty.
        return (tree, state) -> true;
      }
      Node root = new Node();
      rootsBuilder.add(root);
      Node src = root;
      int tokensHandled = 0;
      for (TokenType type : TokenType.values()) {
        Optional<Set<Token>> labels = Optional.ofNullable(required.get(type));
        if (labels.isPresent()) {
          tokensHandled++;
        }
        boolean lastToken = tokensHandled == numTokens;
        Node dst = lastToken ? ACCEPT : new Node();
        if (labels.isPresent()) {
          for (Token label : labels.get()) {
            nfa.put(src, Optional.of(label), dst);
          }
        } else {
          nfa.put(src, Optional.empty(), dst);
        }

        if (lastToken) {
          break; // No transitions out of the accept state.
        }

        src = dst;
      }
    }
    ImmutableSet<Node> roots = rootsBuilder.build();

    // TODO(amalloy): When converting to a DFA, we could use the information that there is
    // exactly one accept state
    // (and no transitions out of that accept state) to help us prune unnecessary identical states
    // from the graph. Instead of starting from the root and proceeding forwards, we could start
    // from the
    // accept state and proceed backwards through the graph. If we ever have two nodes with exactly
    // the same set of outgoing edges, we could replace them with a single node.
    Map<Set<Node>, NodeWithDefault> mappings = new HashMap<>();
    ArrayDeque<Set<Node>> open = new ArrayDeque<>();
    open.add(roots);
    while (!open.isEmpty()) {
      Set<Node> curr = open.removeFirst();
      Set<Node> acceptsAny = new HashSet<>();
      SetMultimap<Token, Node> destinations = HashMultimap.create();

      // First collect all the nodes that accept any token at all
      for (Node node : curr) {
        for (Map.Entry<Optional<Token>, Node> entry : nfa.row(node).entrySet()) {
          if (!entry.getKey().isPresent()) {
            acceptsAny.add(entry.getValue());
          }
        }
      }

      // Then collect the pickier nodes that want a specific token. Now instead of going to that
      // specific node, that token takes us to the union of that node and all "any" nodes.
      for (Node node : curr) {
        for (Map.Entry<Optional<Token>, Node> entry : nfa.row(node).entrySet()) {
          entry
              .getKey()
              .ifPresent(
                  label -> {
                    destinations.put(label, entry.getValue());
                    destinations.putAll(label, acceptsAny);
                  });
        }
      }

      mappings.put(
          curr, new NodeWithDefault(curr, acceptsAny.isEmpty() ? null : acceptsAny, destinations));
      if (!acceptsAny.isEmpty()) {
        open.addLast(acceptsAny);
      }

      // asMap has been @Beta and unmodified for 6 years. I'll take my chances. If it changes, we
      // can just reimplement this simple helper.
      @SuppressWarnings("UnstableApiUsage")
      Collection<Set<Node>> values = Multimaps.asMap(destinations).values();
      open.addAll(values);
    }

    // At this point, mappings has all the information we need, but it's burdened with a bunch of
    // now-unnecessary Set<Node> objects for its map keys. We want to replace those with something
    // lightweight, and convert the indirect references through mappings to a direct pointer lookup.
    return GraphMatcher.from(mappings, mappings.get(roots));
  }

  private MethodInvocationMatcher() {}
}

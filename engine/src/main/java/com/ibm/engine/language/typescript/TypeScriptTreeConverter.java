/*
 * Sonar Cryptography Plugin
 * Copyright (C) 2025 PQCA
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.engine.language.typescript;

import com.ibm.engine.language.typescript.antlr.TypeScriptParser;
import com.ibm.engine.language.typescript.antlr.TypeScriptParserBaseVisitor;
import com.ibm.engine.language.typescript.tree.TypeScriptCallExpressionTree;
import com.ibm.engine.language.typescript.tree.TypeScriptFunctionTree;
import com.ibm.engine.language.typescript.tree.TypeScriptIdentifierTree;
import com.ibm.engine.language.typescript.tree.TypeScriptLiteralTree;
import com.ibm.engine.language.typescript.tree.TypeScriptTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Converts an ANTLR4 TypeScript parse tree to the language-agnostic {@link TypeScriptTree}
 * hierarchy.
 *
 * <p>This visitor walks the parse tree to find function scopes (function declarations, arrow
 * functions, methods) and the top-level program scope. Within each scope it extracts {@link
 * TypeScriptParser.ArgumentsExpressionContext} nodes as {@link TypeScriptCallExpressionTree}
 * instances.
 *
 * <p>A call like {@code crypto.createHash('sha256')} is structured as:
 *
 * <pre>
 *   ArgumentsExpression
 *     MemberDotExpression     (crypto.createHash)
 *       IdentifierExpression  (crypto)
 *       identifierName        (createHash)
 *     arguments               ('sha256')
 * </pre>
 *
 * <p>Uses two visitor layers:
 *
 * <ol>
 *   <li>Outer ({@link TypeScriptTreeConverter}) — walks the whole program and creates one {@link
 *       TypeScriptFunctionTree} per function body and one for the top-level scope.
 *   <li>Inner ({@link CallCollector}) — collects call expressions within a single scope, stopping
 *       at nested function boundaries.
 * </ol>
 */
public final class TypeScriptTreeConverter extends TypeScriptParserBaseVisitor<Void> {

    private final List<TypeScriptFunctionTree> functionTrees = new ArrayList<>();

    /**
     * Extracts all function scopes (including the top-level program scope) from the parse tree.
     *
     * @param root the root {@code program} context
     * @return list of function trees, one per scope found
     */
    @Nonnull
    public List<TypeScriptFunctionTree> extractFunctionScopes(
            @Nonnull TypeScriptParser.ProgramContext root) {
        // Top-level program scope
        collectScope(root, root.getStart() != null ? root.getStart().getLine() : 1, 0);
        // Visit all nested function declarations
        visitChildren(root);
        return Collections.unmodifiableList(functionTrees);
    }

    // -------------------------------------------------------------------------
    // Scope entry points
    // -------------------------------------------------------------------------

    @Override
    public Void visitFunctionDeclaration(TypeScriptParser.FunctionDeclarationContext ctx) {
        if (ctx.functionBody() != null) {
            int line = ctx.getStart().getLine();
            int col = ctx.getStart().getCharPositionInLine();
            collectScope(ctx.functionBody(), line, col);
        }
        visitChildren(ctx);
        return null;
    }

    @Override
    public Void visitAnonymousFunction(TypeScriptParser.AnonymousFunctionContext ctx) {
        // Only handle the anonymous-function-expression form: `function() { ... }`
        // Named function declarations are caught by visitFunctionDeclaration;
        // arrow functions are caught by visitArrowFunctionDeclaration.
        if (ctx.functionBody() != null
                && ctx.functionDeclaration() == null
                && ctx.arrowFunctionDeclaration() == null) {
            int line = ctx.getStart().getLine();
            int col = ctx.getStart().getCharPositionInLine();
            collectScope(ctx.functionBody(), line, col);
        }
        visitChildren(ctx);
        return null;
    }

    @Override
    public Void visitArrowFunctionDeclaration(
            TypeScriptParser.ArrowFunctionDeclarationContext ctx) {
        if (ctx.arrowFunctionBody() != null && ctx.arrowFunctionBody().functionBody() != null) {
            int line = ctx.getStart().getLine();
            int col = ctx.getStart().getCharPositionInLine();
            collectScope(ctx.arrowFunctionBody().functionBody(), line, col);
        }
        visitChildren(ctx);
        return null;
    }

    // -------------------------------------------------------------------------
    // Scope collection
    // -------------------------------------------------------------------------

    private void collectScope(@Nonnull ParseTree scope, int line, int col) {
        CallCollector collector = new CallCollector();
        for (int i = 0; i < scope.getChildCount(); i++) {
            collector.visit(scope.getChild(i));
        }
        functionTrees.add(new TypeScriptFunctionTree(line, col, collector.getCalls()));
    }

    // -------------------------------------------------------------------------
    // Inner visitor: collects call expressions within one scope level
    // -------------------------------------------------------------------------

    /**
     * Collects {@link TypeScriptCallExpressionTree} instances from a single scope, stopping at
     * nested function boundaries so they are picked up by the outer visitor separately.
     */
    private static final class CallCollector extends TypeScriptParserBaseVisitor<Void> {

        private final List<TypeScriptTree> calls = new ArrayList<>();

        /**
         * Pending LHS identifier from a variable declaration ({@code const hash = ...}), consumed
         * by the first ArgumentsExpression seen inside the initializer.
         */
        @Nullable private String pendingAssigned = null;

        @Nonnull
        List<TypeScriptTree> getCalls() {
            return Collections.unmodifiableList(calls);
        }

        // Stop at nested function scopes — they become separate TypeScriptFunctionTree entries
        @Override
        public Void visitFunctionDeclaration(TypeScriptParser.FunctionDeclarationContext ctx) {
            return null;
        }

        @Override
        public Void visitAnonymousFunction(TypeScriptParser.AnonymousFunctionContext ctx) {
            return null;
        }

        @Override
        public Void visitArrowFunctionDeclaration(
                TypeScriptParser.ArrowFunctionDeclarationContext ctx) {
            return null;
        }

        /**
         * Captures the variable name from {@code const hash = createHash(...)} so the emitted tree
         * node gets its {@code assignedIdentifier} populated.
         */
        @Override
        public Void visitVariableDeclaration(TypeScriptParser.VariableDeclarationContext ctx) {
            // variableDeclaration: (identifierOrKeyWord | arrayLiteral | objectLiteral)
            //                      typeAnnotation? singleExpression?
            if (ctx.identifierOrKeyWord() != null) {
                pendingAssigned = ctx.identifierOrKeyWord().getText();
            }
            try {
                return visitChildren(ctx);
            } finally {
                pendingAssigned = null;
            }
        }

        /**
         * Converts an ArgumentsExpression (a call like {@code crypto.createHash('sha256')}) to a
         * {@link TypeScriptCallExpressionTree} and adds it to the list.
         *
         * <p>Does not recurse further to avoid double-counting nested calls at this level; nested
         * calls inside arguments are also captured by visiting children below.
         */
        @Override
        public Void visitArgumentsExpression(TypeScriptParser.ArgumentsExpressionContext ctx) {
            String assigned = pendingAssigned;
            pendingAssigned = null;

            TypeScriptCallExpressionTree call = convertArgumentsExpression(ctx, assigned);
            if (call != null) {
                calls.add(call);
            }

            // Still recurse into arguments so nested calls (e.g. chained) are captured
            if (ctx.arguments() != null) {
                visit(ctx.arguments());
            }
            return null;
        }

        // -----------------------------------------------------------------------
        // Conversion helpers
        // -----------------------------------------------------------------------

        @Nullable private TypeScriptCallExpressionTree convertArgumentsExpression(
                @Nonnull TypeScriptParser.ArgumentsExpressionContext ctx,
                @Nullable String assignedIdentifier) {
            TypeScriptParser.SingleExpressionContext callee = ctx.singleExpression();
            if (callee == null) {
                return null;
            }

            String objectTypeName = "";
            String methodName;

            if (callee instanceof TypeScriptParser.MemberDotExpressionContext memberDot) {
                // e.g. crypto.createHash
                methodName =
                        memberDot.identifierName() != null
                                ? memberDot.identifierName().getText()
                                : "";
                objectTypeName = resolveReceiver(memberDot.singleExpression());
            } else if (callee instanceof TypeScriptParser.IdentifierExpressionContext idExpr) {
                // e.g. createHash(...)
                methodName =
                        idExpr.identifierName() != null ? idExpr.identifierName().getText() : "";
            } else {
                // Other callee types (computed member access, etc.) — not handled
                return null;
            }

            if (methodName.isEmpty()) {
                return null;
            }

            List<TypeScriptTree> args = convertArguments(ctx.arguments());

            return new TypeScriptCallExpressionTree(
                    ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    objectTypeName,
                    methodName,
                    args,
                    assignedIdentifier,
                    null);
        }

        /**
         * Resolves the receiver of a member-dot expression to a simple name string.
         *
         * <p>For {@code crypto.createHash}: receiver = IdentifierExpression("crypto") → "crypto".
         * For chained {@code obj.inner.method}: returns the last qualifier name.
         */
        @Nonnull
        private String resolveReceiver(
                @Nullable TypeScriptParser.SingleExpressionContext receiverCtx) {
            if (receiverCtx == null) {
                return "";
            }
            if (receiverCtx instanceof TypeScriptParser.IdentifierExpressionContext idExpr) {
                return idExpr.identifierName() != null ? idExpr.identifierName().getText() : "";
            }
            if (receiverCtx instanceof TypeScriptParser.MemberDotExpressionContext memberDot) {
                // For chained access: use the inner member name as the "type"
                return memberDot.identifierName() != null
                        ? memberDot.identifierName().getText()
                        : resolveReceiver(memberDot.singleExpression());
            }
            return receiverCtx.getText();
        }

        @Nonnull
        private List<TypeScriptTree> convertArguments(
                @Nullable TypeScriptParser.ArgumentsContext argsCtx) {
            if (argsCtx == null || argsCtx.argumentList() == null) {
                return Collections.emptyList();
            }
            List<TypeScriptTree> result = new ArrayList<>();
            for (TypeScriptParser.ArgumentContext arg : argsCtx.argumentList().argument()) {
                TypeScriptTree argTree = convertArgument(arg);
                if (argTree != null) {
                    result.add(argTree);
                }
            }
            return Collections.unmodifiableList(result);
        }

        @Nullable private TypeScriptTree convertArgument(@Nonnull TypeScriptParser.ArgumentContext arg) {
            if (arg.singleExpression() != null) {
                return convertExpression(arg.singleExpression());
            }
            if (arg.identifier() != null) {
                return new TypeScriptIdentifierTree(
                        arg.getStart().getLine(),
                        arg.getStart().getCharPositionInLine(),
                        arg.identifier().getText());
            }
            return null;
        }

        @Nullable private TypeScriptTree convertExpression(
                @Nullable TypeScriptParser.SingleExpressionContext expr) {
            if (expr == null) {
                return null;
            }
            if (expr instanceof TypeScriptParser.LiteralExpressionContext literalExpr) {
                return convertLiteral(literalExpr.literal());
            }
            if (expr instanceof TypeScriptParser.IdentifierExpressionContext idExpr) {
                String name =
                        idExpr.identifierName() != null ? idExpr.identifierName().getText() : "";
                if (!name.isEmpty()) {
                    return new TypeScriptIdentifierTree(
                            expr.getStart().getLine(),
                            expr.getStart().getCharPositionInLine(),
                            name);
                }
            }
            // Fall back: treat raw text as identifier
            String text = expr.getText();
            if (!text.isEmpty()) {
                return new TypeScriptIdentifierTree(
                        expr.getStart().getLine(), expr.getStart().getCharPositionInLine(), text);
            }
            return null;
        }

        @Nullable private TypeScriptTree convertLiteral(@Nullable TypeScriptParser.LiteralContext literal) {
            if (literal == null) {
                return null;
            }
            int line = literal.getStart().getLine();
            int col = literal.getStart().getCharPositionInLine();

            if (literal.StringLiteral() != null) {
                String raw = literal.StringLiteral().getText();
                // Strip surrounding quotes (' or ")
                String value = raw;
                if (raw.length() >= 2
                        && (raw.charAt(0) == '\''
                                || raw.charAt(0) == '"'
                                || raw.charAt(0) == '`')) {
                    value = raw.substring(1, raw.length() - 1);
                }
                return new TypeScriptLiteralTree(
                        line, col, TypeScriptLiteralTree.Kind.STRING, value);
            }
            if (literal.numericLiteral() != null) {
                return new TypeScriptLiteralTree(
                        line,
                        col,
                        TypeScriptLiteralTree.Kind.NUMBER,
                        literal.numericLiteral().getText());
            }
            if (literal.BooleanLiteral() != null) {
                return new TypeScriptLiteralTree(
                        line,
                        col,
                        TypeScriptLiteralTree.Kind.BOOLEAN,
                        literal.BooleanLiteral().getText());
            }
            if (literal.NullLiteral() != null) {
                return new TypeScriptLiteralTree(
                        line, col, TypeScriptLiteralTree.Kind.NULL, "null");
            }
            return null;
        }
    }
}

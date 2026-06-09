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
package com.ibm.engine.language.typescript.tree;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a TypeScript call expression such as {@code crypto.createHash('sha256')} or {@code
 * createHash('sha256')}.
 *
 * <p>{@code objectTypeName} is the receiver/qualifier before the dot (e.g. {@code "crypto"}), and
 * {@code methodName} is the function name (e.g. {@code "createHash"}). For unqualified calls,
 * {@code objectTypeName} is an empty string.
 */
public final class TypeScriptCallExpressionTree implements TypeScriptTree {

    private final int line;
    private final int column;

    /** The receiver name before the dot, or empty string for unqualified calls. */
    @Nonnull private final String objectTypeName;

    /** The function/method name being called. */
    @Nonnull private final String methodName;

    /** The argument trees in call order. */
    @Nonnull private final List<TypeScriptTree> arguments;

    /** Optional identifier this call result is assigned to (for depending-rule tracking). */
    @Nullable private final String assignedIdentifier;

    /** The enclosing function scope (back-patched by TypeScriptFunctionTree). */
    @Nullable private TypeScriptFunctionTree enclosingFunction;

    public TypeScriptCallExpressionTree(
            int line,
            int column,
            @Nonnull String objectTypeName,
            @Nonnull String methodName,
            @Nonnull List<TypeScriptTree> arguments,
            @Nullable String assignedIdentifier,
            @Nullable TypeScriptFunctionTree enclosingFunction) {
        this.line = line;
        this.column = column;
        this.objectTypeName = objectTypeName;
        this.methodName = methodName;
        this.arguments = arguments;
        this.assignedIdentifier = assignedIdentifier;
        this.enclosingFunction = enclosingFunction;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Nonnull
    @Override
    public String getText() {
        return objectTypeName.isEmpty()
                ? methodName + "(...)"
                : objectTypeName + "." + methodName + "(...)";
    }

    @Nonnull
    public String getObjectTypeName() {
        return objectTypeName;
    }

    @Nonnull
    public String getMethodName() {
        return methodName;
    }

    @Nonnull
    public List<TypeScriptTree> getArguments() {
        return arguments;
    }

    @Nullable public String getAssignedIdentifier() {
        return assignedIdentifier;
    }

    @Nullable public TypeScriptFunctionTree getEnclosingFunction() {
        return enclosingFunction;
    }

    /** Back-patched by {@link TypeScriptFunctionTree} once the function scope is built. */
    public void setEnclosingFunction(@Nonnull TypeScriptFunctionTree enclosingFunction) {
        this.enclosingFunction = enclosingFunction;
    }
}

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

/**
 * Represents a TypeScript function scope (function body or top-level program scope) containing a
 * list of call expressions found within it.
 *
 * <p>This is the entry point for the detection engine — the sensor dispatches one {@code
 * TypeScriptFunctionTree} per function (or per file for top-level code) to the detection checks.
 */
public final class TypeScriptFunctionTree implements TypeScriptTree {

    private final int line;
    private final int column;
    @Nonnull private final List<TypeScriptTree> statements;

    public TypeScriptFunctionTree(int line, int column, @Nonnull List<TypeScriptTree> statements) {
        this.line = line;
        this.column = column;
        this.statements = statements;
        for (TypeScriptTree statement : statements) {
            if (statement instanceof TypeScriptCallExpressionTree call) {
                call.setEnclosingFunction(this);
            }
        }
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
        return "<function>";
    }

    @Nonnull
    public List<TypeScriptTree> getStatements() {
        return statements;
    }
}

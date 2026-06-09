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

import com.ibm.engine.detection.IType;
import com.ibm.engine.detection.MatchContext;
import com.ibm.engine.language.ILanguageTranslation;
import com.ibm.engine.language.typescript.tree.TypeScriptCallExpressionTree;
import com.ibm.engine.language.typescript.tree.TypeScriptIdentifierTree;
import com.ibm.engine.language.typescript.tree.TypeScriptLiteralTree;
import com.ibm.engine.language.typescript.tree.TypeScriptTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Language translation implementation for TypeScript.
 *
 * <p>Extracts method names and object type strings from the {@link TypeScriptTree} hierarchy. Since
 * ANTLR4 provides only syntactic information (no type inference), all parameter types match any
 * expected type.
 */
public final class TypeScriptLanguageTranslation implements ILanguageTranslation<TypeScriptTree> {

    @Nonnull
    @Override
    public Optional<String> getMethodName(
            @Nonnull MatchContext matchContext, @Nonnull TypeScriptTree methodInvocation) {
        if (methodInvocation instanceof TypeScriptCallExpressionTree call) {
            return Optional.of(call.getMethodName());
        }
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<IType> getInvokedObjectTypeString(
            @Nonnull MatchContext matchContext, @Nonnull TypeScriptTree methodInvocation) {
        if (methodInvocation instanceof TypeScriptCallExpressionTree call) {
            String typeName = call.getObjectTypeName();
            return Optional.of(expectedType -> expectedType.equals(typeName));
        }
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<IType> getMethodReturnTypeString(
            @Nonnull MatchContext matchContext, @Nonnull TypeScriptTree methodInvocation) {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public List<IType> getMethodParameterTypes(
            @Nonnull MatchContext matchContext, @Nonnull TypeScriptTree methodInvocation) {
        if (methodInvocation instanceof TypeScriptCallExpressionTree call) {
            List<TypeScriptTree> args = call.getArguments();
            if (!args.isEmpty()) {
                List<IType> types = new ArrayList<>(args.size());
                for (int i = 0; i < args.size(); i++) {
                    types.add(expectedType -> true);
                }
                return types;
            }
        }
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Optional<String> resolveIdentifierAsString(
            @Nonnull MatchContext matchContext, @Nonnull TypeScriptTree identifierTree) {
        if (identifierTree instanceof TypeScriptLiteralTree literal) {
            return Optional.of(literal.getValue());
        } else if (identifierTree instanceof TypeScriptIdentifierTree identifier) {
            return Optional.of(identifier.getName());
        }
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<String> getEnumIdentifierName(
            @Nonnull MatchContext matchContext, @Nonnull TypeScriptTree enumIdentifier) {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<String> getEnumClassName(
            @Nonnull MatchContext matchContext, @Nonnull TypeScriptTree enumClass) {
        return Optional.empty();
    }
}

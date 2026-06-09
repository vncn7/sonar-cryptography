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

import com.ibm.engine.detection.DetectionStore;
import com.ibm.engine.detection.EnumMatcher;
import com.ibm.engine.detection.Handler;
import com.ibm.engine.detection.IBaseMethodVisitorFactory;
import com.ibm.engine.detection.IDetectionEngine;
import com.ibm.engine.detection.MatchContext;
import com.ibm.engine.detection.MethodMatcher;
import com.ibm.engine.executive.DetectionExecutive;
import com.ibm.engine.language.ILanguageSupport;
import com.ibm.engine.language.ILanguageTranslation;
import com.ibm.engine.language.IScanContext;
import com.ibm.engine.language.typescript.tree.TypeScriptCallExpressionTree;
import com.ibm.engine.language.typescript.tree.TypeScriptTree;
import com.ibm.engine.rule.IDetectionRule;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Language support implementation for TypeScript. */
public final class TypeScriptLanguageSupport
        implements ILanguageSupport<
                TypeScriptCheck, TypeScriptTree, TypeScriptSymbol, TypeScriptScanContext> {

    @Nonnull
    private final Handler<TypeScriptCheck, TypeScriptTree, TypeScriptSymbol, TypeScriptScanContext>
            handler;

    @Nonnull private final TypeScriptLanguageTranslation translation;

    public TypeScriptLanguageSupport() {
        this.handler = new Handler<>(this);
        this.translation = new TypeScriptLanguageTranslation();
    }

    @Nonnull
    @Override
    public ILanguageTranslation<TypeScriptTree> translation() {
        return translation;
    }

    @Nonnull
    @Override
    public DetectionExecutive<
                    TypeScriptCheck, TypeScriptTree, TypeScriptSymbol, TypeScriptScanContext>
            createDetectionExecutive(
                    @Nonnull TypeScriptTree tree,
                    @Nonnull IDetectionRule<TypeScriptTree> detectionRule,
                    @Nonnull IScanContext<TypeScriptCheck, TypeScriptTree> scanContext) {
        return new DetectionExecutive<>(tree, detectionRule, scanContext, this.handler);
    }

    @Nonnull
    @Override
    public IDetectionEngine<TypeScriptTree, TypeScriptSymbol> createDetectionEngineInstance(
            @Nonnull
                    DetectionStore<
                                    TypeScriptCheck,
                                    TypeScriptTree,
                                    TypeScriptSymbol,
                                    TypeScriptScanContext>
                            detectionStore) {
        return new TypeScriptDetectionEngine(detectionStore, this.handler);
    }

    @Nonnull
    @Override
    public IBaseMethodVisitorFactory<TypeScriptTree, TypeScriptSymbol>
            getBaseMethodVisitorFactory() {
        return TypeScriptBaseMethodVisitor::new;
    }

    @Nonnull
    @Override
    public Optional<TypeScriptTree> getEnclosingMethod(@Nonnull TypeScriptTree expression) {
        if (expression instanceof TypeScriptCallExpressionTree call
                && call.getEnclosingFunction() != null) {
            return Optional.of(call.getEnclosingFunction());
        }
        return Optional.empty();
    }

    @Nullable @Override
    public MethodMatcher<TypeScriptTree> createMethodMatcherBasedOn(
            @Nonnull TypeScriptTree methodDefinition) {
        return null;
    }

    @Nullable @Override
    public EnumMatcher<TypeScriptTree> createSimpleEnumMatcherFor(
            @Nonnull TypeScriptTree enumIdentifier, @Nonnull MatchContext matchContext) {
        Optional<String> name = translation().getEnumIdentifierName(matchContext, enumIdentifier);
        return name.<EnumMatcher<TypeScriptTree>>map(EnumMatcher::new).orElse(null);
    }
}

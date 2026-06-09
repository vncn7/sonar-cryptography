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
package com.ibm.plugin.rules.detection;

import com.ibm.common.IObserver;
import com.ibm.engine.detection.Finding;
import com.ibm.engine.executive.DetectionExecutive;
import com.ibm.engine.language.typescript.TypeScriptCheck;
import com.ibm.engine.language.typescript.TypeScriptScanContext;
import com.ibm.engine.language.typescript.TypeScriptSymbol;
import com.ibm.engine.language.typescript.tree.TypeScriptFunctionTree;
import com.ibm.engine.language.typescript.tree.TypeScriptTree;
import com.ibm.engine.rule.IDetectionRule;
import com.ibm.mapper.model.INode;
import com.ibm.mapper.reorganizer.IReorganizerRule;
import com.ibm.plugin.TypeScriptAggregator;
import com.ibm.plugin.translation.TypeScriptTranslationProcess;
import com.ibm.plugin.translation.reorganizer.TypeScriptReorganizerRules;
import com.ibm.rules.IReportableDetectionRule;
import com.ibm.rules.issue.Issue;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Abstract base class for TypeScript cryptographic detection rules.
 *
 * <p>Implements {@link TypeScriptCheck}: for every function scope dispatched by the sensor, all
 * configured {@link IDetectionRule}s are run and their findings translated.
 */
public abstract class TypeScriptBaseDetectionRule
        implements TypeScriptCheck,
                IObserver<
                        Finding<
                                TypeScriptCheck,
                                TypeScriptTree,
                                TypeScriptSymbol,
                                TypeScriptScanContext>>,
                IReportableDetectionRule<TypeScriptTree> {

    private final boolean isInventory;
    @Nonnull protected final TypeScriptTranslationProcess translationProcess;
    @Nonnull protected final List<IDetectionRule<TypeScriptTree>> detectionRules;

    protected TypeScriptBaseDetectionRule() {
        this.isInventory = false;
        this.detectionRules = TypeScriptDetectionRules.rules();
        this.translationProcess =
                new TypeScriptTranslationProcess(TypeScriptReorganizerRules.rules());
    }

    protected TypeScriptBaseDetectionRule(
            final boolean isInventory,
            @Nonnull List<IDetectionRule<TypeScriptTree>> detectionRules,
            @Nonnull List<IReorganizerRule> reorganizerRules) {
        this.isInventory = isInventory;
        this.detectionRules = detectionRules;
        this.translationProcess = new TypeScriptTranslationProcess(reorganizerRules);
    }

    @Override
    public void scan(
            @Nonnull TypeScriptScanContext scanContext,
            @Nonnull TypeScriptFunctionTree functionTree) {
        // Process each call expression in its own executive so we emit one finding per matched
        // call, mirroring how the Java plugin visits each method-call node independently.
        for (TypeScriptTree statement : functionTree.getStatements()) {
            TypeScriptFunctionTree singleCallScope =
                    new TypeScriptFunctionTree(
                            statement.getLine(), statement.getColumn(), List.of(statement));
            detectionRules.forEach(
                    rule -> {
                        DetectionExecutive<
                                        TypeScriptCheck,
                                        TypeScriptTree,
                                        TypeScriptSymbol,
                                        TypeScriptScanContext>
                                executive =
                                        TypeScriptAggregator.getLanguageSupport()
                                                .createDetectionExecutive(
                                                        singleCallScope, rule, scanContext);
                        executive.subscribe(this);
                        executive.start();
                    });
        }
    }

    @Override
    public void update(
            @Nonnull
                    Finding<
                                    TypeScriptCheck,
                                    TypeScriptTree,
                                    TypeScriptSymbol,
                                    TypeScriptScanContext>
                            finding) {
        List<INode> nodes = translationProcess.initiate(finding.detectionStore());
        if (isInventory) {
            TypeScriptAggregator.addNodes(nodes);
        }
        this.report(finding.getMarkerTree(), nodes)
                .forEach(
                        issue ->
                                finding.detectionStore()
                                        .getScanContext()
                                        .reportIssue(this, issue.tree(), issue.message()));
    }

    @Override
    @Nonnull
    public List<Issue<TypeScriptTree>> report(
            @Nonnull TypeScriptTree markerTree, @Nonnull List<INode> translatedNodes) {
        return Collections.emptyList();
    }
}

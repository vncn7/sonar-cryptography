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
package com.ibm.plugin;

import com.ibm.engine.detection.DetectionStore;
import com.ibm.engine.detection.Finding;
import com.ibm.engine.language.typescript.TypeScriptCheck;
import com.ibm.engine.language.typescript.TypeScriptScanContext;
import com.ibm.engine.language.typescript.TypeScriptSymbol;
import com.ibm.engine.language.typescript.tree.TypeScriptTree;
import com.ibm.engine.model.IValue;
import com.ibm.engine.rule.IDetectionRule;
import com.ibm.engine.utils.DetectionStoreLogger;
import com.ibm.mapper.model.INode;
import com.ibm.plugin.rules.TypeScriptInventoryRule;
import com.ibm.plugin.rules.detection.TypeScriptDetectionRules;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TestBase extends TypeScriptInventoryRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestBase.class);

    @Nonnull
    private final DetectionStoreLogger<
                    TypeScriptCheck, TypeScriptTree, TypeScriptSymbol, TypeScriptScanContext>
            detectionStoreLogger = new DetectionStoreLogger<>();

    private int findingId = 0;

    public TestBase(@Nonnull List<IDetectionRule<TypeScriptTree>> detectionRules) {
        super(detectionRules);
    }

    public TestBase() {
        super(TypeScriptDetectionRules.rules());
    }

    @BeforeEach
    public void resetState() {
        TypeScriptAggregator.reset();
    }

    @BeforeEach
    public void resetNodeTreeLog() {
        try {
            Path logFile = Paths.get("target/node-tree.log");
            Files.createDirectories(logFile.getParent());
            Files.writeString(
                    logFile,
                    "=== Node Tree Log: " + getClass().getSimpleName() + " ===\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not reset node-tree.log", e);
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
        final DetectionStore<
                        TypeScriptCheck, TypeScriptTree, TypeScriptSymbol, TypeScriptScanContext>
                detectionStore = finding.detectionStore();
        detectionStoreLogger.print(detectionStore);

        final List<INode> nodes = translationProcess.initiate(detectionStore);
        writeNodeTree(findingId, nodes);
        asserts(findingId, detectionStore, nodes);
        findingId++;
        this.report(finding.getMarkerTree(), nodes)
                .forEach(
                        issue ->
                                finding.detectionStore()
                                        .getScanContext()
                                        .reportIssue(this, issue.tree(), issue.message()));
    }

    public abstract void asserts(
            int findingId,
            @Nonnull
                    DetectionStore<
                                    TypeScriptCheck,
                                    TypeScriptTree,
                                    TypeScriptSymbol,
                                    TypeScriptScanContext>
                            detectionStore,
            @Nonnull List<INode> nodes);

    private void writeNodeTree(int id, @Nonnull List<INode> nodes) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\n[findingId=").append(id).append("] Node tree:\n");
            nodes.forEach(node -> appendNodeTree(sb, node, 0));
            Files.writeString(
                    Paths.get("target/node-tree.log"),
                    sb.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.warn("Could not write node-tree.log", e);
        }
    }

    private void appendNodeTree(@Nonnull StringBuilder sb, @Nonnull INode node, int depth) {
        String indent = "   ".repeat(depth) + (depth > 0 ? "└─ " : "");
        sb.append(indent)
                .append(node.getKind().getSimpleName())
                .append("  \"")
                .append(node.asString())
                .append("\"  [")
                .append(node.getOrigin())
                .append("]\n");
        node.getChildren().values().forEach(child -> appendNodeTree(sb, child, depth + 1));
    }

    @Nullable public DetectionStore<TypeScriptCheck, TypeScriptTree, TypeScriptSymbol, TypeScriptScanContext>
            getStoreOfValueType(
                    @Nonnull Class<? extends IValue> valueType,
                    @Nonnull
                            List<
                                            DetectionStore<
                                                    TypeScriptCheck,
                                                    TypeScriptTree,
                                                    TypeScriptSymbol,
                                                    TypeScriptScanContext>>
                                    detectionStores) {
        Optional<
                        DetectionStore<
                                TypeScriptCheck,
                                TypeScriptTree,
                                TypeScriptSymbol,
                                TypeScriptScanContext>>
                relevantStore =
                        detectionStores.stream()
                                .filter(
                                        store ->
                                                store.getDetectionValues().stream()
                                                        .anyMatch(
                                                                value ->
                                                                        value.getClass()
                                                                                .equals(valueType)))
                                .findFirst();
        return relevantStore.orElseGet(
                () ->
                        detectionStores.stream()
                                .map(
                                        store ->
                                                Optional.ofNullable(
                                                        getStoreOfValueType(
                                                                valueType, store.getChildren())))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .findFirst()
                                .orElse(null));
    }
}

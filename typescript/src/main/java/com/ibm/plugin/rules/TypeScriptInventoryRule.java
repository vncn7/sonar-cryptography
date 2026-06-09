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
package com.ibm.plugin.rules;

import com.ibm.engine.language.typescript.tree.TypeScriptTree;
import com.ibm.engine.rule.IDetectionRule;
import com.ibm.mapper.model.INode;
import com.ibm.plugin.rules.detection.TypeScriptBaseDetectionRule;
import com.ibm.plugin.rules.detection.TypeScriptDetectionRules;
import com.ibm.plugin.translation.reorganizer.TypeScriptReorganizerRules;
import com.ibm.rules.InventoryRule;
import com.ibm.rules.issue.Issue;
import java.util.List;
import javax.annotation.Nonnull;
import org.sonar.check.Rule;
import org.sonar.java.annotations.VisibleForTesting;

/** The single TypeScript cryptographic inventory rule — detects all known crypto API usage. */
@Rule(key = "Inventory")
public class TypeScriptInventoryRule extends TypeScriptBaseDetectionRule {

    public TypeScriptInventoryRule() {
        super(true, TypeScriptDetectionRules.rules(), TypeScriptReorganizerRules.rules());
    }

    @VisibleForTesting
    protected TypeScriptInventoryRule(
            @Nonnull List<IDetectionRule<TypeScriptTree>> detectionRules) {
        super(true, detectionRules, TypeScriptReorganizerRules.rules());
    }

    @Override
    public @Nonnull List<Issue<TypeScriptTree>> report(
            @Nonnull TypeScriptTree markerTree, @Nonnull List<INode> translatedNodes) {
        return new InventoryRule<TypeScriptTree>().report(markerTree, translatedNodes);
    }
}

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
package com.ibm.plugin.rules.detection.nodejs;

import com.ibm.engine.detection.MethodMatcher;
import com.ibm.engine.language.typescript.tree.TypeScriptTree;
import com.ibm.engine.model.context.DigestContext;
import com.ibm.engine.model.factory.AlgorithmFactory;
import com.ibm.engine.rule.IDetectionRule;
import com.ibm.engine.rule.builder.DetectionRuleBuilder;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Detection rule for {@code crypto.createHash(algorithm)} from the Node.js built-in {@code crypto}
 * module.
 *
 * <p>Example: {@code crypto.createHash('sha256')}
 */
public final class NodeJsCryptoCreateHash {

    private NodeJsCryptoCreateHash() {
        // nothing
    }

    private static final IDetectionRule<TypeScriptTree> CREATE_HASH =
            new DetectionRuleBuilder<TypeScriptTree>()
                    .createDetectionRule()
                    .forObjectTypes("crypto")
                    .forMethods("createHash")
                    .withMethodParameter(MethodMatcher.ANY)
                    .shouldBeDetectedAs(new AlgorithmFactory<>())
                    .buildForContext(new DigestContext())
                    .inBundle(() -> "NodeJs")
                    .withoutDependingDetectionRules();

    @Nonnull
    public static List<IDetectionRule<TypeScriptTree>> rules() {
        return List.of(CREATE_HASH);
    }
}

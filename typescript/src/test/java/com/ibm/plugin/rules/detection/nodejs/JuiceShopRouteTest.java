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

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.engine.detection.DetectionStore;
import com.ibm.engine.language.typescript.TypeScriptCheck;
import com.ibm.engine.language.typescript.TypeScriptScanContext;
import com.ibm.engine.language.typescript.TypeScriptSymbol;
import com.ibm.engine.language.typescript.tree.TypeScriptTree;
import com.ibm.engine.model.Algorithm;
import com.ibm.engine.model.IValue;
import com.ibm.engine.model.context.DigestContext;
import com.ibm.mapper.model.INode;
import com.ibm.mapper.model.MessageDigest;
import com.ibm.plugin.TestBase;
import com.ibm.plugin.TypeScriptVerifier;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

/**
 * Verifies that crypto.createHash() calls are detected inside typed arrow-function middleware,
 * and that `import x = require('...')` no longer causes parse errors.
 */
class JuiceShopRouteTest extends TestBase {

    @Test
    void test() throws Exception {
        TypeScriptVerifier.verify("rules/detection/nodejs/JuiceShopRouteTestFile.ts", this);
    }

    @Override
    public void asserts(
            int findingId,
            @Nonnull
                    DetectionStore<
                                    TypeScriptCheck,
                                    TypeScriptTree,
                                    TypeScriptSymbol,
                                    TypeScriptScanContext>
                            detectionStore,
            @Nonnull List<INode> nodes) {

        assertThat(detectionStore.getDetectionValues()).hasSize(1);
        assertThat(detectionStore.getDetectionValueContext()).isInstanceOf(DigestContext.class);
        IValue<TypeScriptTree> value0 = detectionStore.getDetectionValues().get(0);
        assertThat(value0).isInstanceOf(Algorithm.class);

        assertThat(nodes).hasSize(1);
        INode node = nodes.get(0);
        assertThat(node.getKind()).isEqualTo(MessageDigest.class);

        switch (findingId) {
            case 0 -> {
                assertThat(value0.asString()).isEqualTo("sha256");
                assertThat(node.asString()).isEqualTo("SHA256");
            }
            default -> throw new IllegalStateException("Unexpected findingId: " + findingId);
        }
    }
}

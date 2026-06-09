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

import com.ibm.engine.detection.IBaseMethodVisitor;
import com.ibm.engine.detection.IDetectionEngine;
import com.ibm.engine.detection.TraceSymbol;
import com.ibm.engine.language.typescript.tree.TypeScriptFunctionTree;
import com.ibm.engine.language.typescript.tree.TypeScriptTree;
import javax.annotation.Nonnull;

/** Base method visitor for TypeScript: invokes the detection engine on each function scope. */
public final class TypeScriptBaseMethodVisitor implements IBaseMethodVisitor<TypeScriptTree> {

    @Nonnull private final TraceSymbol<TypeScriptSymbol> traceSymbol;
    @Nonnull private final IDetectionEngine<TypeScriptTree, TypeScriptSymbol> detectionEngine;

    public TypeScriptBaseMethodVisitor(
            @Nonnull TraceSymbol<TypeScriptSymbol> traceSymbol,
            @Nonnull IDetectionEngine<TypeScriptTree, TypeScriptSymbol> detectionEngine) {
        this.traceSymbol = traceSymbol;
        this.detectionEngine = detectionEngine;
    }

    @Override
    public void visitMethodDefinition(@Nonnull TypeScriptTree method) {
        if (method instanceof TypeScriptFunctionTree functionTree) {
            detectionEngine.run(traceSymbol, functionTree);
        }
    }
}

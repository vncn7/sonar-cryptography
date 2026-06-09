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
package com.ibm.plugin.translation.translator;

import com.ibm.engine.language.typescript.TypeScriptCheck;
import com.ibm.engine.language.typescript.TypeScriptScanContext;
import com.ibm.engine.language.typescript.TypeScriptSymbol;
import com.ibm.engine.language.typescript.tree.TypeScriptCallExpressionTree;
import com.ibm.engine.language.typescript.tree.TypeScriptTree;
import com.ibm.engine.model.Algorithm;
import com.ibm.engine.model.IValue;
import com.ibm.engine.model.context.DigestContext;
import com.ibm.engine.model.context.IDetectionContext;
import com.ibm.engine.rule.IBundle;
import com.ibm.mapper.ITranslator;
import com.ibm.mapper.mapper.jca.JcaMessageDigestMapper;
import com.ibm.mapper.model.INode;
import com.ibm.mapper.model.functionality.Digest;
import com.ibm.mapper.utils.DetectionLocation;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Translator for TypeScript cryptographic detections. Dispatches to context-specific logic. */
public class TypeScriptTranslator
        extends ITranslator<
                TypeScriptCheck, TypeScriptTree, TypeScriptSymbol, TypeScriptScanContext> {

    @Nonnull
    @Override
    public Optional<INode> translate(
            @Nonnull final IBundle bundleIdentifier,
            @Nonnull IValue<TypeScriptTree> value,
            @Nonnull IDetectionContext detectionValueContext,
            @Nonnull final String filePath) {
        DetectionLocation detectionLocation =
                getDetectionContextFrom(value.getLocation(), bundleIdentifier, filePath);
        if (detectionLocation == null) {
            return Optional.empty();
        }

        if (detectionValueContext.is(DigestContext.class)) {
            if (value instanceof Algorithm<?>) {
                JcaMessageDigestMapper digestMapper = new JcaMessageDigestMapper();
                return digestMapper
                        .parse(value.asString(), detectionLocation)
                        .map(
                                algo -> {
                                    algo.put(new Digest(detectionLocation));
                                    return (INode) algo;
                                });
            }
        }

        return Optional.empty();
    }

    @Override
    @Nullable protected DetectionLocation getDetectionContextFrom(
            @Nonnull TypeScriptTree location, @Nonnull IBundle bundle, @Nonnull String filePath) {
        int lineNumber = location.getLine();
        int offset = location.getColumn();

        List<String> keywords = List.of();
        if (location instanceof TypeScriptCallExpressionTree call) {
            keywords = List.of(call.getMethodName());
        }

        return new DetectionLocation(filePath, lineNumber, offset, keywords, bundle);
    }
}

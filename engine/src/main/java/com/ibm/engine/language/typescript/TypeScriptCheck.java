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

import com.ibm.engine.language.typescript.tree.TypeScriptFunctionTree;
import javax.annotation.Nonnull;

/**
 * Marker interface for TypeScript cryptography detection checks.
 *
 * <p>Since sonar-javascript exposes no custom rule registration API compatible with our
 * ANTLR4-based approach, {@link com.ibm.plugin.CryptoTypeScriptSensor} calls {@link #scan} directly
 * for every function scope it encounters during parsing.
 */
public interface TypeScriptCheck {

    /**
     * Invoked once per function scope (or top-level program scope) found in a TypeScript source
     * file.
     *
     * @param scanContext the current scan context (input file, sensor context, repository key)
     * @param functionTree the function scope to analyse
     */
    void scan(
            @Nonnull TypeScriptScanContext scanContext,
            @Nonnull TypeScriptFunctionTree functionTree);
}

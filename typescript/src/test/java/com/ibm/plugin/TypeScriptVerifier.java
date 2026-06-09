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

import com.ibm.engine.language.typescript.TypeScriptCheck;
import com.ibm.engine.language.typescript.TypeScriptScanContext;
import com.ibm.engine.language.typescript.TypeScriptTreeConverter;
import com.ibm.engine.language.typescript.antlr.TypeScriptLexer;
import com.ibm.engine.language.typescript.antlr.TypeScriptParser;
import com.ibm.engine.language.typescript.tree.TypeScriptFunctionTree;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.annotation.Nonnull;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

/**
 * ANTLR-based verifier for TypeScript detection rule tests.
 *
 * <p>Parses a {@code .ts} test file using the ANTLR4 TypeScript grammar, extracts all function
 * scopes, and runs the given {@link TypeScriptCheck} against each scope. Findings are observed
 * directly via the check's internal observer subscription — no native SonarQube binary required.
 *
 * <p>Usage: {@code TypeScriptVerifier.verify("rules/detection/nodejs/MyTestFile.ts", this);} where
 * the path is relative to {@code src/test/files/}.
 */
public final class TypeScriptVerifier {

    private static final String TEST_FILES_ROOT = "src/test/files/";

    private TypeScriptVerifier() {
        // utility
    }

    /**
     * Parses the given test file and runs the check against all discovered function scopes.
     *
     * @param relativeTestFilePath path to the .ts file relative to {@code src/test/files/}
     * @param check the check instance to run (typically a {@code TestBase} subclass)
     */
    public static void verify(@Nonnull String relativeTestFilePath, @Nonnull TypeScriptCheck check)
            throws IOException {
        Path filePath = Paths.get(TEST_FILES_ROOT + relativeTestFilePath);
        String content = Files.readString(filePath, StandardCharsets.UTF_8);

        TypeScriptLexer lexer =
                new TypeScriptLexer(CharStreams.fromString(content, filePath.toString()));
        lexer.removeErrorListeners();
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TypeScriptParser parser = new TypeScriptParser(tokens);
        parser.removeErrorListeners();
        TypeScriptParser.ProgramContext parseTree = parser.program();

        TypeScriptTreeConverter converter = new TypeScriptTreeConverter();
        List<TypeScriptFunctionTree> functionScopes = converter.extractFunctionScopes(parseTree);

        Path moduleRoot = Paths.get(".").toAbsolutePath().normalize();
        SensorContextTester sensorContext = SensorContextTester.create(moduleRoot);

        InputFile inputFile =
                TestInputFileBuilder.create("test-module", filePath.toString())
                        .setContents(content)
                        .setCharset(StandardCharsets.UTF_8)
                        .setLanguage("ts")
                        .setType(InputFile.Type.MAIN)
                        .build();
        sensorContext.fileSystem().add(inputFile);

        TypeScriptScanContext scanContext =
                new TypeScriptScanContext(
                        sensorContext, inputFile, TypeScriptScannerRuleDefinition.REPOSITORY_KEY);

        for (TypeScriptFunctionTree functionTree : functionScopes) {
            check.scan(scanContext, functionTree);
        }
    }
}

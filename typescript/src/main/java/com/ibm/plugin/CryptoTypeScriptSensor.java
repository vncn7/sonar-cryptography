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
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;

/**
 * Custom sensor for executing cryptography detection rules on TypeScript source files.
 *
 * <p>Like the C# sensor, TypeScript has no public SonarQube custom rule API, so each .ts file is
 * parsed independently with the ANTLR4-based TypeScriptLexer/TypeScriptParser, and detection checks
 * are invoked directly for every function scope found.
 */
public class CryptoTypeScriptSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(CryptoTypeScriptSensor.class);

    private final Collection<TypeScriptCheck> checks;

    public CryptoTypeScriptSensor(@Nonnull CheckFactory checkFactory) {
        this.checks =
                checkFactory
                        .<TypeScriptCheck>create(TypeScriptScannerRuleDefinition.REPOSITORY_KEY)
                        .addAnnotatedChecks(TypeScriptRuleList.getChecks())
                        .all();
    }

    @Override
    public void describe(@Nonnull SensorDescriptor descriptor) {
        descriptor.onlyOnLanguage("ts").name("Cryptography for TypeScript");
    }

    @Override
    public void execute(@Nonnull SensorContext context) {
        if (checks.isEmpty()) {
            return;
        }

        FileSystem fs = context.fileSystem();
        Iterable<InputFile> tsFiles =
                fs.inputFiles(
                        fs.predicates()
                                .and(
                                        fs.predicates().hasLanguage("ts"),
                                        fs.predicates().hasType(InputFile.Type.MAIN)));

        for (InputFile inputFile : tsFiles) {
            if (context.isCancelled()) {
                return;
            }
            analyzeFile(context, inputFile);
        }
    }

    private void analyzeFile(@Nonnull SensorContext context, @Nonnull InputFile inputFile) {
        String content;
        try {
            content = inputFile.contents();
        } catch (IOException e) {
            LOG.warn("Unable to read file: {}", inputFile, e);
            return;
        }

        TypeScriptParser.ProgramContext parseTree = parseContent(content, inputFile);
        if (parseTree == null) {
            return;
        }

        TypeScriptTreeConverter converter = new TypeScriptTreeConverter();
        List<TypeScriptFunctionTree> functionScopes = converter.extractFunctionScopes(parseTree);
        if (functionScopes.isEmpty()) {
            return;
        }

        TypeScriptScanContext scanContext =
                new TypeScriptScanContext(
                        context, inputFile, TypeScriptScannerRuleDefinition.REPOSITORY_KEY);

        for (TypeScriptFunctionTree functionTree : functionScopes) {
            for (TypeScriptCheck check : checks) {
                try {
                    check.scan(scanContext, functionTree);
                } catch (RuntimeException e) {
                    LOG.warn(
                            "Error running check {} on {}: {}",
                            check.getClass().getSimpleName(),
                            inputFile,
                            e.getMessage(),
                            e);
                }
            }
        }
    }

    @Nullable private TypeScriptParser.ProgramContext parseContent(
            @Nonnull String content, @Nonnull InputFile inputFile) {
        try {
            TypeScriptLexer lexer =
                    new TypeScriptLexer(CharStreams.fromString(content, inputFile.toString()));
            lexer.removeErrorListeners();
            lexer.addErrorListener(new TypeScriptParserErrorListener(inputFile));

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            TypeScriptParser parser = new TypeScriptParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(new TypeScriptParserErrorListener(inputFile));

            return parser.program();
        } catch (RuntimeException e) {
            LOG.warn("Unable to parse file: {}", inputFile, e);
            return null;
        }
    }
}

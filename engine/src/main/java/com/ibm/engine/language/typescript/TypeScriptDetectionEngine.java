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

import com.ibm.engine.detection.DetectionStore;
import com.ibm.engine.detection.Handler;
import com.ibm.engine.detection.IDetectionEngine;
import com.ibm.engine.detection.MethodDetection;
import com.ibm.engine.detection.ResolvedValue;
import com.ibm.engine.detection.TraceSymbol;
import com.ibm.engine.detection.ValueDetection;
import com.ibm.engine.language.typescript.tree.TypeScriptCallExpressionTree;
import com.ibm.engine.language.typescript.tree.TypeScriptFunctionTree;
import com.ibm.engine.language.typescript.tree.TypeScriptIdentifierTree;
import com.ibm.engine.language.typescript.tree.TypeScriptLiteralTree;
import com.ibm.engine.language.typescript.tree.TypeScriptTree;
import com.ibm.engine.model.factory.IValueFactory;
import com.ibm.engine.rule.DetectableParameter;
import com.ibm.engine.rule.DetectionRule;
import com.ibm.engine.rule.MethodDetectionRule;
import com.ibm.engine.rule.Parameter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Detection engine implementation for TypeScript.
 *
 * <p>Walks a {@link TypeScriptFunctionTree} looking for {@link TypeScriptCallExpressionTree} nodes
 * that match the active detection rule, then emits detections and resolves argument values.
 *
 * <p>Symbol resolution is minimal: only string/number literals and direct identifiers are resolved.
 */
@SuppressWarnings("java:S3776")
public final class TypeScriptDetectionEngine
        implements IDetectionEngine<TypeScriptTree, TypeScriptSymbol> {

    @Nonnull
    private final DetectionStore<
                    TypeScriptCheck, TypeScriptTree, TypeScriptSymbol, TypeScriptScanContext>
            detectionStore;

    @Nonnull
    private final Handler<TypeScriptCheck, TypeScriptTree, TypeScriptSymbol, TypeScriptScanContext>
            handler;

    public TypeScriptDetectionEngine(
            @Nonnull
                    DetectionStore<
                                    TypeScriptCheck,
                                    TypeScriptTree,
                                    TypeScriptSymbol,
                                    TypeScriptScanContext>
                            detectionStore,
            @Nonnull
                    Handler<
                                    TypeScriptCheck,
                                    TypeScriptTree,
                                    TypeScriptSymbol,
                                    TypeScriptScanContext>
                            handler) {
        this.detectionStore = detectionStore;
        this.handler = handler;
    }

    @Override
    public void run(@Nonnull TypeScriptTree tree) {
        run(TraceSymbol.createStart(), tree);
    }

    @Override
    public void run(
            @Nonnull TraceSymbol<TypeScriptSymbol> traceSymbol, @Nonnull TypeScriptTree tree) {
        if (tree instanceof TypeScriptFunctionTree functionTree) {
            for (TypeScriptTree statement : functionTree.getStatements()) {
                processStatement(traceSymbol, statement);
            }
        } else if (tree instanceof TypeScriptCallExpressionTree call) {
            if (traceSymbol.is(TraceSymbol.State.SYMBOL)
                    && !isInvocationOnVariable(call, traceSymbol)) {
                return;
            }
            handler.addCallToCallStack(call, detectionStore.getScanContext());
            if (detectionStore
                    .getDetectionRule()
                    .match(call, handler.getLanguageSupport().translation())) {
                analyseCall(call);
            }
        }
    }

    private void processStatement(
            @Nonnull TraceSymbol<TypeScriptSymbol> traceSymbol, @Nonnull TypeScriptTree statement) {
        if (!(statement instanceof TypeScriptCallExpressionTree call)) {
            return;
        }
        if (traceSymbol.is(TraceSymbol.State.SYMBOL)
                && !isInvocationOnVariable(call, traceSymbol)) {
            return;
        }
        handler.addCallToCallStack(call, detectionStore.getScanContext());
        if (detectionStore
                .getDetectionRule()
                .match(call, handler.getLanguageSupport().translation())) {
            analyseCall(call);
        }
    }

    @SuppressWarnings("unchecked")
    private void analyseCall(@Nonnull TypeScriptCallExpressionTree call) {
        if (detectionStore.getDetectionRule().is(MethodDetectionRule.class)) {
            detectionStore.onReceivingNewDetection(new MethodDetection<>(call, null));
            return;
        }
        DetectionRule<TypeScriptTree> rule =
                (DetectionRule<TypeScriptTree>) detectionStore.getDetectionRule();
        if (rule.actionFactory() != null) {
            detectionStore.onReceivingNewDetection(new MethodDetection<>(call, null));
        }
        List<TypeScriptTree> arguments = call.getArguments();
        List<Parameter<TypeScriptTree>> parameters = rule.parameters();
        int index = 0;
        for (Parameter<TypeScriptTree> parameter : parameters) {
            if (index >= arguments.size()) {
                break;
            }
            processParameter(parameter, arguments.get(index), call);
            index++;
        }
    }

    @SuppressWarnings("unchecked")
    private void processParameter(
            @Nonnull Parameter<TypeScriptTree> parameter,
            @Nonnull TypeScriptTree expression,
            @Nonnull TypeScriptTree parentTree) {
        if (parameter.is(DetectableParameter.class)) {
            DetectableParameter<TypeScriptTree> detectable =
                    (DetectableParameter<TypeScriptTree>) parameter;
            List<ResolvedValue<Object, TypeScriptTree>> resolved =
                    resolveValuesInInnerScope(
                            Object.class, expression, detectable.getiValueFactory());
            if (resolved.isEmpty()) {
                resolveValuesInOuterScope(expression, detectable);
            } else {
                resolved.stream()
                        .map(rv -> new ValueDetection<>(rv, detectable, parentTree, parentTree))
                        .forEach(detectionStore::onReceivingNewDetection);
            }
        } else if (!parameter.getDetectionRules().isEmpty()) {
            detectionStore.onDetectedDependingParameter(
                    parameter, expression, DetectionStore.Scope.EXPRESSION);
        }
    }

    @Nonnull
    @Override
    public <O> List<ResolvedValue<O, TypeScriptTree>> resolveValuesInInnerScope(
            @Nonnull Class<O> clazz,
            @Nonnull TypeScriptTree expression,
            @Nullable IValueFactory<TypeScriptTree> valueFactory) {
        return resolveValues(clazz, expression);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private <O> List<ResolvedValue<O, TypeScriptTree>> resolveValues(
            @Nonnull Class<O> clazz, @Nonnull TypeScriptTree tree) {
        if (tree instanceof TypeScriptLiteralTree literal) {
            Optional<O> resolved = resolveConstant(clazz, literal.getValue());
            return resolved.map(v -> List.of(new ResolvedValue<>(v, tree)))
                    .orElse(Collections.emptyList());
        }
        if (tree instanceof TypeScriptIdentifierTree identifier) {
            Optional<O> resolved = resolveConstant(clazz, identifier.getName());
            return resolved.map(v -> List.of(new ResolvedValue<>(v, tree)))
                    .orElse(Collections.emptyList());
        }
        return Collections.emptyList();
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    private <O> Optional<O> resolveConstant(@Nonnull Class<O> clazz, @Nullable String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            if (clazz == String.class) {
                return Optional.of(clazz.cast(value));
            }
            if (clazz == Integer.class || clazz == Object.class) {
                try {
                    Integer intValue = Integer.parseInt(value);
                    if (clazz == Integer.class) {
                        return Optional.of(clazz.cast(intValue));
                    }
                    return Optional.of((O) intValue);
                } catch (NumberFormatException e) {
                    // not a number
                }
            }
            if (clazz == Object.class) {
                return Optional.of((O) value);
            }
            return Optional.empty();
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }

    @Override
    public void resolveValuesInOuterScope(
            @Nonnull TypeScriptTree expression, @Nonnull Parameter<TypeScriptTree> parameter) {
        // Cross-scope resolution not supported without semantic analysis
    }

    @Override
    public <O> void resolveMethodReturnValues(
            @Nonnull Class<O> clazz,
            @Nonnull TypeScriptTree methodDefinition,
            @Nonnull Parameter<TypeScriptTree> parameter) {
        // Return value resolution not supported without type inference
    }

    @Nullable @Override
    public <O> ResolvedValue<O, TypeScriptTree> resolveEnumValue(
            @Nonnull Class<O> clazz,
            @Nonnull TypeScriptTree enumClassDefinition,
            @Nonnull LinkedList<TypeScriptTree> selections) {
        return null;
    }

    @Nonnull
    @Override
    public Optional<TraceSymbol<TypeScriptSymbol>> getAssignedSymbol(
            @Nonnull TypeScriptTree expression) {
        if (expression instanceof TypeScriptCallExpressionTree call) {
            String assigned = call.getAssignedIdentifier();
            if (assigned != null) {
                return Optional.of(TraceSymbol.createFrom(new TypeScriptSymbol(assigned)));
            }
        }
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<TraceSymbol<TypeScriptSymbol>> getMethodInvocationParameterSymbol(
            @Nonnull TypeScriptTree methodInvocation,
            @Nonnull Parameter<TypeScriptTree> parameter) {
        if (methodInvocation instanceof TypeScriptCallExpressionTree call) {
            int idx = parameter.getIndex();
            if (idx >= 0 && idx < call.getArguments().size()) {
                return Optional.of(TraceSymbol.createWithStateNoSymbol());
            }
            return Optional.of(TraceSymbol.createWithStateDifferent());
        }
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<TraceSymbol<TypeScriptSymbol>> getNewClassParameterSymbol(
            @Nonnull TypeScriptTree newClass, @Nonnull Parameter<TypeScriptTree> parameter) {
        return Optional.of(TraceSymbol.createWithStateDifferent());
    }

    @Override
    public boolean isInvocationOnVariable(
            TypeScriptTree methodInvocation,
            @Nonnull TraceSymbol<TypeScriptSymbol> variableSymbol) {
        if (!(methodInvocation instanceof TypeScriptCallExpressionTree call)) {
            return false;
        }
        TypeScriptSymbol sym = variableSymbol.getSymbol();
        if (sym == null) {
            return false;
        }
        return call.getObjectTypeName().equals(sym.getName());
    }

    @Override
    public boolean isInitForVariable(
            TypeScriptTree newClass, @Nonnull TraceSymbol<TypeScriptSymbol> variableSymbol) {
        if (!(newClass instanceof TypeScriptCallExpressionTree call)) {
            return false;
        }
        String assignedId = call.getAssignedIdentifier();
        if (assignedId == null) {
            return false;
        }
        TypeScriptSymbol sym = variableSymbol.getSymbol();
        return sym != null && assignedId.equals(sym.getName());
    }

    @Nullable @Override
    public TypeScriptTree extractArgumentFromMethodCaller(
            @Nonnull TypeScriptTree methodDefinition,
            @Nonnull TypeScriptTree methodInvocation,
            @Nonnull TypeScriptTree methodParameterIdentifier) {
        return null;
    }
}

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

import javax.annotation.Nonnull;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;

/**
 * Custom sensor for executing cryptography detection rules on TypeScript source files.
 *
 * <p>No detection rules are registered yet; this sensor is a placeholder that declares the plugin's
 * interest in TypeScript files and will be populated with checks in a future iteration.
 */
public class CryptoTypeScriptSensor implements Sensor {

    @Override
    public void describe(@Nonnull SensorDescriptor descriptor) {
        descriptor.onlyOnLanguage("ts").name("Cryptography for TypeScript");
    }

    @Override
    public void execute(@Nonnull SensorContext context) {
        // No detection rules registered yet
    }
}

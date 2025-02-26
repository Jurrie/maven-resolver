/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.resolver.examples.supplier;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.generator.gnupg.GnupgSignatureArtifactGeneratorFactory;
import org.eclipse.aether.generator.gnupg.loaders.*;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.spi.artifact.decorator.ArtifactDecorator;
import org.eclipse.aether.spi.artifact.decorator.ArtifactDecoratorFactory;
import org.eclipse.aether.spi.artifact.generator.ArtifactGeneratorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transport.jdk.JdkTransporterFactory;
import org.eclipse.aether.transport.jetty.JettyTransporterFactory;

/**
 * A factory for repository system instances that employs Maven Artifact Resolver's provided supplier.
 */
public class SupplierRepositorySystemFactory {
    public static RepositorySystem newRepositorySystem() {
        return new RepositorySystemSupplier() {
            @Override
            protected Map<String, ArtifactGeneratorFactory> createArtifactGeneratorFactories() {
                Map<String, ArtifactGeneratorFactory> result = super.createArtifactGeneratorFactories();
                result.put(
                        GnupgSignatureArtifactGeneratorFactory.NAME,
                        new GnupgSignatureArtifactGeneratorFactory(
                                getArtifactPredicateFactory(), getGnupgSignatureArtifactGeneratorFactoryLoaders()));
                return result;
            }

            private Map<String, GnupgSignatureArtifactGeneratorFactory.Loader>
                    getGnupgSignatureArtifactGeneratorFactoryLoaders() {
                // order matters
                LinkedHashMap<String, GnupgSignatureArtifactGeneratorFactory.Loader> loaders = new LinkedHashMap<>();
                loaders.put(GpgEnvLoader.NAME, new GpgEnvLoader());
                loaders.put(GpgConfLoader.NAME, new GpgConfLoader());
                loaders.put(GpgAgentPasswordLoader.NAME, new GpgAgentPasswordLoader());
                return loaders;
            }

            @Override
            protected Map<String, ArtifactDecoratorFactory> createArtifactDecoratorFactories() {
                Map<String, ArtifactDecoratorFactory> result = super.createArtifactDecoratorFactories();
                result.put("color", new ArtifactDecoratorFactory() {
                    @Override
                    public ArtifactDecorator newInstance(RepositorySystemSession session) {
                        return new ArtifactDecorator() {
                            @Override
                            public Artifact decorateArtifact(ArtifactDescriptorResult artifactDescriptorResult) {
                                Map<String, String> properties = new HashMap<>(
                                        artifactDescriptorResult.getArtifact().getProperties());
                                properties.put("color", "red");
                                return artifactDescriptorResult.getArtifact().setProperties(properties);
                            }
                        };
                    }

                    @Override
                    public float getPriority() {
                        return 0;
                    }
                });
                return result;
            }

            @Override
            protected Map<String, TransporterFactory> createTransporterFactories() {
                Map<String, TransporterFactory> result = super.createTransporterFactories();
                result.put(
                        JdkTransporterFactory.NAME,
                        new JdkTransporterFactory(getChecksumExtractor(), getPathProcessor()));
                result.put(JettyTransporterFactory.NAME, new JettyTransporterFactory(getChecksumExtractor()));
                return result;
            }
        }.get();
    }
}

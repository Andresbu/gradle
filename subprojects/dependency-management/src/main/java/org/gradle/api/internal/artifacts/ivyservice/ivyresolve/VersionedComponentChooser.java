/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.artifacts.ivyservice.ivyresolve;

import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.internal.Factory;
import org.gradle.internal.component.model.ComponentResolveMetaData;
import org.gradle.internal.component.model.DependencyMetaData;
import org.gradle.internal.resolve.result.BuildableModuleComponentMetaDataResolveResult;
import org.gradle.internal.resolve.result.BuildableComponentSelectionResult;
import org.gradle.internal.resolve.result.ModuleVersionListing;

public interface VersionedComponentChooser {
    ComponentResolveMetaData selectNewestComponent(ComponentResolveMetaData one, ComponentResolveMetaData two);
    void selectNewestMatchingComponent(ModuleVersionListing versions, DependencyMetaData dependency, ModuleComponentRepositoryAccess moduleAccess, BuildableComponentSelectionResult result);
    boolean isRejectedComponent(ModuleComponentIdentifier candidateIdentifier, Factory<? extends BuildableModuleComponentMetaDataResolveResult> metaDataSupplier);
}

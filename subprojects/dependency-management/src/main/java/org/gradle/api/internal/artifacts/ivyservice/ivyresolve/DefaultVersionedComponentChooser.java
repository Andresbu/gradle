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

import org.gradle.api.artifacts.ComponentSelection;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.internal.artifacts.ComponentSelectionInternal;
import org.gradle.api.internal.artifacts.ComponentSelectionRulesInternal;
import org.gradle.api.internal.artifacts.DefaultComponentSelection;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionComparator;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionSelector;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionSelectorScheme;
import org.gradle.internal.Factory;
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier;
import org.gradle.internal.component.model.ComponentResolveMetaData;
import org.gradle.internal.component.model.DependencyMetaData;
import org.gradle.internal.resolve.result.BuildableComponentSelectionResult;
import org.gradle.internal.resolve.result.BuildableModuleComponentMetaDataResolveResult;
import org.gradle.internal.resolve.result.ModuleVersionListing;
import org.gradle.internal.rules.SpecRuleAction;
import org.gradle.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.gradle.api.internal.artifacts.ivyservice.ivyresolve.MetadataProvider.MetaDataSupplier;

class DefaultVersionedComponentChooser implements VersionedComponentChooser {
    private final ComponentSelectionRulesProcessor rulesProcessor = new ComponentSelectionRulesProcessor();
    private final VersionSelectorScheme versionSelectorScheme;
    private final VersionComparator versionComparator;
    private final ComponentSelectionRulesInternal componentSelectionRules;

    DefaultVersionedComponentChooser(VersionComparator versionComparator, VersionSelectorScheme versionSelectorScheme, ComponentSelectionRulesInternal componentSelectionRules) {
        this.versionComparator = versionComparator;
        this.versionSelectorScheme = versionSelectorScheme;
        this.componentSelectionRules = componentSelectionRules;
    }

    public ComponentResolveMetaData selectNewestComponent(ComponentResolveMetaData one, ComponentResolveMetaData two) {
        if (one == null || two == null) {
            return two == null ? one : two;
        }

        int comparison = versionComparator.compare(new VersionInfo(one.getId().getVersion()), new VersionInfo(two.getId().getVersion()));

        if (comparison == 0) {
            if (isGeneratedModuleDescriptor(one) && !isGeneratedModuleDescriptor(two)) {
                return two;
            }
            return one;
        }

        return comparison < 0 ? two : one;
    }

    private boolean isGeneratedModuleDescriptor(ComponentResolveMetaData componentResolveMetaData) {
        return componentResolveMetaData.isGenerated();
    }

    public void selectNewestMatchingComponent(ModuleVersionListing versions, DependencyMetaData dependency, ModuleComponentRepositoryAccess moduleAccess, BuildableComponentSelectionResult result) {
        ModuleVersionSelector requestedModule = dependency.getRequested();
        VersionSelector requestedVersion = versionSelectorScheme.parseSelector(requestedModule.getVersion());
        Collection<SpecRuleAction<? super ComponentSelection>> rules = componentSelectionRules.getRules();

        for (Versioned candidate : sortLatestFirst(versions)) {
            ModuleComponentIdentifier candidateIdentifier = DefaultModuleComponentIdentifier.newId(requestedModule.getGroup(), requestedModule.getName(), candidate.getVersion());
            MetadataProvider metadataProvider = new MetadataProvider(new MetaDataSupplier(dependency, candidateIdentifier, moduleAccess));

            if(requestedVersion.requiresMetadata() && !metadataProvider.canProvideMetaData()) {
                return;
            }

            if (versionMatches(requestedVersion, candidateIdentifier, metadataProvider)) {
                if (!isRejectedByRules(candidateIdentifier, rules, metadataProvider)) {
                    result.matches(candidateIdentifier);
                    return;
                }

                if (requestedVersion.matchesUniqueVersion()) {
                    break;
                }
            }
        }

        result.noMatchFound();
    }

    private boolean versionMatches(VersionSelector selector, ModuleComponentIdentifier candidateIdentifier, MetadataProvider metadataProvider) {
        if (selector.requiresMetadata()) {
            return selector.accept(metadataProvider.getComponentMetadata());
        } else {
            return selector.accept(candidateIdentifier.getVersion());
        }
    }

    public boolean isRejectedComponent(ModuleComponentIdentifier candidateIdentifier, Factory<? extends BuildableModuleComponentMetaDataResolveResult> metaDataSupplier) {
        return isRejectedByRules(candidateIdentifier, componentSelectionRules.getRules(), new MetadataProvider(metaDataSupplier));
    }

    private boolean isRejectedByRules(ModuleComponentIdentifier candidateIdentifier, Collection<SpecRuleAction<? super ComponentSelection>> rules, MetadataProvider metadataProvider) {
        ComponentSelectionInternal selection = new DefaultComponentSelection(candidateIdentifier);
        rulesProcessor.apply(selection, rules, metadataProvider);
        return selection.isRejected();
    }

    private List<Versioned> sortLatestFirst(ModuleVersionListing listing) {
        return CollectionUtils.sort(listing.getVersions(), Collections.reverseOrder(versionComparator));
    }
}

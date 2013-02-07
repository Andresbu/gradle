/*
 * Copyright 2012 the original author or authors.
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
package org.gradle.api.publish.maven.internal.artifact

import org.gradle.api.Buildable
import org.gradle.api.Task
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.publish.maven.MavenArtifact
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.reflect.DirectInstantiator
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.HelperUtil
import spock.lang.Specification

public class MavenArtifactNotationParserTest extends Specification {
    Instantiator instantiator = new DirectInstantiator()
    TaskDependency taskDependency = Mock()
    FileResolver fileResolver = Mock()
    PublishArtifact publishArtifact = Stub() {
        getExtension() >> 'extension'
        getClassifier() >> 'classifier'
        getFile() >> new File('foo')
        getBuildDependencies() >> taskDependency
    }

    MavenArtifactNotationParser parser = new MavenArtifactNotationParser(instantiator, "1.2", fileResolver)

    def "directly returns MavenArtifact input"() {
        when:
        MavenArtifact mavenArtifact = Mock()
        def output = parser.parseNotation(mavenArtifact)

        then:
        output == mavenArtifact
    }

    def "creates MavenArtifact for PublishArtifact"() {
        when:
        def mavenArtifact = parser.parseNotation(publishArtifact)

        then:
        mavenArtifact.extension == publishArtifact.extension
        mavenArtifact.classifier == publishArtifact.classifier
        mavenArtifact.file == publishArtifact.file

        and:
        mavenArtifact instanceof Buildable
        mavenArtifact.buildDependencies == taskDependency
    }

    def "creates MavenArtifact for source map notation"() {
        when:
        MavenArtifact mavenArtifact = parser.parseNotation(source: publishArtifact)

        then:
        mavenArtifact.extension == publishArtifact.extension
        mavenArtifact.classifier == publishArtifact.classifier
        mavenArtifact.file == publishArtifact.file

        and:
        mavenArtifact instanceof Buildable
        mavenArtifact.buildDependencies == taskDependency
    }

    def "creates and configures MavenArtifact for source map notation"() {
        when:
        MavenArtifact mavenArtifact = parser.parseNotation(source: publishArtifact, extension: "ext", classifier: "classy")

        then:
        mavenArtifact.file == publishArtifact.file
        mavenArtifact.extension == "ext"
        mavenArtifact.classifier == "classy"

        and:
        mavenArtifact instanceof Buildable
        mavenArtifact.buildDependencies == taskDependency
    }

    def "creates MavenArtifact for ArchivePublishArtifact"() {
        when:
        def rootProject = HelperUtil.createRootProject()
        def archive = rootProject.task(type: Jar, {})
        archive.setBaseName("baseName")
        archive.setExtension(archiveExtension)
        archive.setClassifier(archiveClassifier)

        MavenArtifact mavenArtifact = parser.parseNotation(archive)

        then:
        mavenArtifact.extension == artifactExtension
        mavenArtifact.classifier == artifactClassifier
        mavenArtifact.file == archive.archivePath
        mavenArtifact instanceof Buildable
        (mavenArtifact as Buildable).buildDependencies.getDependencies(null) == [archive] as Set

        where:
        archiveClassifier | artifactClassifier | archiveExtension | artifactExtension
        "classifier"      | "classifier"       | "extension"      | "extension"
        null              | ""                 | null             | ""
    }

    def "creates MavenArtifact for file notation"() {
        given:
        File file = new File('some-file-1.2-classifier.zip')

        when:
        MavenArtifact mavenArtifact = parser.parseNotation('some-file')

        then:
        fileResolver.resolve('some-file') >> file

        and:
        mavenArtifact.extension == "zip"
        mavenArtifact.classifier == "classifier"
        mavenArtifact.file == file

        where:
        fileName                       | extension | classifier
        "some-file"                    | ""        | ""
        "some-file.zip"                | "zip"     | ""
        "some-file-1.2-classifier.zip" | "zip"     | "classifier"
    }

    def "creates MavenArtifact for file map notation"() {
        given:
        File file = new File('some-file-1.2-classifier.zip')

        when:
        MavenArtifact mavenArtifact = parser.parseNotation(file: 'some-file')

        then:
        fileResolver.resolve('some-file') >> file

        and:
        mavenArtifact.extension == "zip"
        mavenArtifact.classifier == "classifier"
        mavenArtifact.file == file
    }

    def "creates MavenArtifact for source map notation with file"() {
        given:
        File file = new File('some-file-1.2-classifier.zip')

        when:
        MavenArtifact mavenArtifact = parser.parseNotation(source: 'some-file')

        then:
        fileResolver.resolve('some-file') >> file

        and:
        mavenArtifact.extension == "zip"
        mavenArtifact.classifier == "classifier"
        mavenArtifact.file == file
    }

    def "creates and configures MavenArtifact for file map notation"() {
        given:
        File file = new File('some-file-1.2-classifier.zip')
        Task task = Mock()

        when:
        MavenArtifact mavenArtifact = parser.parseNotation(file: 'some-file', extension: "ext", classifier: "classy", builtBy: task)

        then:
        fileResolver.resolve('some-file') >> file

        and:
        mavenArtifact.file == file
        mavenArtifact.extension == "ext"
        mavenArtifact.classifier == "classy"
        mavenArtifact.buildDependencies.getDependencies(Mock(Task)) == [task] as Set
    }
}

/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2026 The JReleaser authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.model.internal.validation.packagers;

import org.jreleaser.logging.SimpleJReleaserLoggerAdapter;
import org.jreleaser.model.Active;
import org.jreleaser.model.Distribution.DistributionType;
import org.jreleaser.model.api.JReleaserCommand;
import org.jreleaser.model.api.JReleaserContext.Mode;
import org.jreleaser.model.internal.JReleaserContext;
import org.jreleaser.model.internal.JReleaserModel;
import org.jreleaser.model.internal.common.Artifact;
import org.jreleaser.model.internal.distributions.Distribution;
import org.jreleaser.model.internal.packagers.BrewPackager;
import org.jreleaser.model.internal.release.GithubReleaser;
import org.jreleaser.util.Errors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Harsh Mehta
 * @since 1.26.0
 */
class BrewPackagerValidatorTest {

    @Test
    void brewDisabledWithHintWhenJavaBinaryDistributionHasJarArtifact(@TempDir Path tempDir) {
        JReleaserContext context = createContext(tempDir);

        Distribution distribution = new Distribution();
        distribution.setName("myapp");
        distribution.setActive(Active.ALWAYS);
        distribution.setType(DistributionType.JAVA_BINARY);

        Artifact artifact = new Artifact();
        artifact.setPath("myapp-1.0.jar");
        artifact.setActive(Active.ALWAYS);
        distribution.addArtifact(artifact);

        BrewPackager packager = new BrewPackager();
        packager.setActive(Active.ALWAYS);

        context.getModel().getPackagers().getBrew().setActive(Active.ALWAYS);

        Errors errors = new Errors();
        BrewPackagerValidator.validateBrew(context, distribution, packager, errors);

        assertTrue(errors.hasWarnings(), "Expected warnings for JAVA_BINARY + jar artifact");
        assertFalse(packager.isEnabled(), "Expected brew packager to be disabled");

        String warnings = errors.warningsAsString();
        assertTrue(warnings.contains("distribution.myapp.brew was disabled because there are no matching artifacts"),
            "Expected generic disable warning: " + warnings);
        assertTrue(warnings.contains(".jar") && warnings.contains("JAVA_BINARY"),
            "Expected hint mentioning .jar and JAVA_BINARY: " + warnings);
        assertTrue(warnings.contains("SINGLE_JAR"),
            "Expected hint to suggest SINGLE_JAR as compatible type: " + warnings);
    }

    @Test
    void brewNotDisabledWhenJavaBinaryDistributionHasZipArtifact(@TempDir Path tempDir) {
        JReleaserContext context = createContext(tempDir);

        Distribution distribution = new Distribution();
        distribution.setName("myapp");
        distribution.setActive(Active.ALWAYS);
        distribution.setType(DistributionType.JAVA_BINARY);

        Artifact artifact = new Artifact();
        artifact.setPath("myapp-1.0.zip");
        artifact.setActive(Active.ALWAYS);
        distribution.addArtifact(artifact);

        BrewPackager packager = new BrewPackager();
        packager.setActive(Active.ALWAYS);

        context.getModel().getPackagers().getBrew().setActive(Active.ALWAYS);

        Errors errors = new Errors();
        BrewPackagerValidator.validateBrew(context, distribution, packager, errors);

        assertFalse(errors.warningsAsString().contains("SINGLE_JAR"),
            "Expected no SINGLE_JAR hint when .zip artifact is present");
    }

    private static JReleaserContext createContext(Path basedir) {
        JReleaserModel model = new JReleaserModel();
        model.getRelease().setGithub(new GithubReleaser());

        return new JReleaserContext(
            new SimpleJReleaserLoggerAdapter(SimpleJReleaserLoggerAdapter.Level.WARN),
            JReleaserContext.Configurer.CLI_YAML,
            Mode.FULL,
            JReleaserCommand.FULL_RELEASE,
            model,
            basedir,
            basedir.resolve("settings.properties"),
            basedir.resolve("out/jreleaser"),
            false,
            true,
            true,
            false,
            false,
            Collections.emptyList(),
            Collections.emptyList());
    }
}

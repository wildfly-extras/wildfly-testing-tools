/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.tools.module;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link ModuleDescription}.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@SuppressWarnings("EqualsWithItself")
class ModuleDescriptionTest {

    @Test
    void createAliasWithExplicitModulePath(@TempDir Path tempDir) throws IOException {
        final Path moduleXml;
        final Path modulePath;

        try (ModuleDescription desc = ModuleDescription.createAlias(tempDir, "org.test.alias", "org.test.target")) {
            Assertions.assertNotNull(desc);
            Assertions.assertEquals("org.test.alias", desc.name());
            Assertions.assertNotNull(desc.modulePath());
            Assertions.assertTrue(Files.exists(desc.modulePath()));

            // Verify module.xml was created
            moduleXml = desc.modulePath().resolve("module.xml");
            modulePath = desc.modulePath();
            Assertions.assertTrue(Files.exists(moduleXml));
        }

        // After close, module.xml should be deleted
        Assertions.assertFalse(Files.exists(moduleXml), "module.xml should be deleted after close");
        // The module directory itself may or may not be deleted depending on whether it's empty
    }

    @Test
    void createAliasWithCustomPath(@TempDir Path tempDir) throws IOException {
        final Path customModulePath = tempDir.resolve("custom-modules");
        Files.createDirectories(customModulePath);

        try (ModuleDescription desc = ModuleDescription.createAlias(customModulePath, "org.test.custom",
                "org.test.custom.target")) {
            Assertions.assertNotNull(desc);
            Assertions.assertEquals("org.test.custom", desc.name());
            Assertions.assertTrue(desc.modulePath().startsWith(customModulePath));
            Assertions.assertTrue(Files.exists(desc.modulePath()));
        }
    }

    @Test
    void equalsAndHashCode(@TempDir Path tempDir) {
        try (
                ModuleDescription desc1 = ModuleDescription.createAlias(tempDir, "org.test.mod1", "org.test.target1");
                ModuleDescription desc2 = ModuleDescription.createAlias(tempDir, "org.test.mod1", "org.test.target1");
                ModuleDescription desc3 = ModuleDescription.createAlias(tempDir, "org.test.mod2", "org.test.target2")) {
            // Same name and path = equals
            Assertions.assertEquals(desc1, desc2);
            Assertions.assertEquals(desc1.hashCode(), desc2.hashCode());

            // Different name or path = not equals
            Assertions.assertNotEquals(desc1, desc3);

            // Reflexive
            Assertions.assertEquals(desc1, desc1);
        }
    }

    @Test
    void compareTo(@TempDir Path tempDir) {

        try (
                ModuleDescription a = ModuleDescription.createAlias(tempDir, "org.a.module", "org.a.target");
                ModuleDescription b = ModuleDescription.createAlias(tempDir, "org.b.module", "org.b.target");
                ModuleDescription c = ModuleDescription.createAlias(tempDir, "org.c.module", "org.c.target")) {
            // Natural ordering by name
            Assertions.assertTrue(a.compareTo(b) < 0);
            Assertions.assertTrue(b.compareTo(a) > 0);
            Assertions.assertEquals(0, a.compareTo(a));

            // Use in sorted collection
            final Set<ModuleDescription> sorted = new TreeSet<>();
            sorted.add(c);
            sorted.add(a);
            sorted.add(b);

            final var list = sorted.stream().toList();
            Assertions.assertEquals(a, list.get(0));
            Assertions.assertEquals(b, list.get(1));
            Assertions.assertEquals(c, list.get(2));
        }
    }

    @Test
    void toStringContainsRelevantInfo(@TempDir Path tempDir) {

        try (ModuleDescription desc = ModuleDescription.createAlias(tempDir, "org.test.tostring", "org.test.target")) {
            final String str = desc.toString();
            Assertions.assertTrue(str.contains("org.test.tostring"));
            Assertions.assertTrue(str.contains("modulePath="));
        }
    }

    @Test
    void closeWithExistingFiles(@TempDir Path tempDir) throws IOException {
        final Path extraFile;
        final Path moduleXml;

        try (ModuleDescription desc = ModuleDescription.createAlias(tempDir, "org.test.withfiles", "org.test.target")) {
            // Add an additional file to the module directory
            extraFile = desc.modulePath().resolve("extra.txt");
            moduleXml = desc.modulePath().resolve("module.xml");
            Files.writeString(extraFile, "test content");
            Assertions.assertTrue(Files.exists(extraFile));
            Assertions.assertTrue(Files.exists(moduleXml));
        }

        // After close, both files should be deleted
        Assertions.assertFalse(Files.exists(extraFile), "extra.txt should be deleted after close");
        Assertions.assertFalse(Files.exists(moduleXml), "module.xml should be deleted after close");
    }

    @Test
    void closeWithNestedDirectories(@TempDir Path tempDir) throws IOException {
        final Path subdir;
        final Path subdirFile;
        final Path moduleXml;

        try (ModuleDescription desc = ModuleDescription.createAlias(tempDir, "org.test.nested", "org.test.target")) {
            // Create a nested directory structure
            subdir = desc.modulePath().resolve("subdir");
            Files.createDirectories(subdir);
            subdirFile = subdir.resolve("file.txt");
            moduleXml = desc.modulePath().resolve("module.xml");
            Files.writeString(subdirFile, "content");
            Assertions.assertTrue(Files.exists(subdirFile));
        }

        // After close, module.xml should be deleted
        // The subdirectory and its file may remain since close() only deletes files in the module directory itself
        Assertions.assertFalse(Files.exists(moduleXml), "module.xml should be deleted after close");
    }

    @Test
    void modulePathGetter(@TempDir Path tempDir) {

        try (ModuleDescription desc = ModuleDescription.createAlias(tempDir, "org.test.getter", "org.test.target")) {
            final Path path = desc.modulePath();
            Assertions.assertNotNull(path);
            Assertions.assertTrue(path.toString().contains("org"));
            Assertions.assertTrue(path.toString().contains("test"));
            Assertions.assertTrue(path.toString().contains("getter"));
            Assertions.assertTrue(path.toString().endsWith("main"));
        }
    }

    @Test
    void nameGetter(@TempDir Path tempDir) {

        try (ModuleDescription desc = ModuleDescription.createAlias(tempDir, "org.test.name.getter", "org.test.target")) {
            Assertions.assertEquals("org.test.name.getter", desc.name());
        }
    }
}

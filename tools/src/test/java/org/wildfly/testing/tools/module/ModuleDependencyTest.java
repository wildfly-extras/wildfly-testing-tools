/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.testing.tools.module;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleDependency}.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@SuppressWarnings("EqualsWithItself")
class ModuleDependencyTest {

    @Test
    void equalsAndHashCode() {
        final ModuleDependency dep1 = ModuleDependency.builder("org.jboss.logging").build();
        final ModuleDependency dep2 = ModuleDependency.builder("org.jboss.logging").build();
        final ModuleDependency dep3 = ModuleDependency.builder("org.jboss.logging")
                .export(true)
                .optional(true)
                .build();
        final ModuleDependency dep4 = ModuleDependency.builder("org.wildfly.common").build();

        // Same name = equals (regardless of other properties)
        Assertions.assertEquals(dep1, dep2);
        Assertions.assertEquals(dep1.hashCode(), dep2.hashCode());

        // Same name even with different properties
        Assertions.assertEquals(dep1, dep3);
        Assertions.assertEquals(dep1.hashCode(), dep3.hashCode());

        // Different name = not equals
        Assertions.assertNotEquals(dep1, dep4);

        // Reflexive
        Assertions.assertEquals(dep1, dep1);
    }

    @Test
    void compareTo() {
        final ModuleDependency a = ModuleDependency.builder("a.module").build();
        final ModuleDependency b = ModuleDependency.builder("b.module").build();
        final ModuleDependency c = ModuleDependency.builder("c.module").build();

        // Natural ordering
        Assertions.assertTrue(a.compareTo(b) < 0);
        Assertions.assertTrue(b.compareTo(a) > 0);
        Assertions.assertEquals(0, a.compareTo(a));

        // Use in sorted collection
        final Set<ModuleDependency> sorted = new TreeSet<>();
        sorted.add(c);
        sorted.add(a);
        sorted.add(b);

        final var list = sorted.stream().toList();
        Assertions.assertEquals(a, list.get(0));
        Assertions.assertEquals(b, list.get(1));
        Assertions.assertEquals(c, list.get(2));
    }

    @Test
    void builderProperties() {
        final ModuleDependency dep = ModuleDependency.builder("org.jboss.logging")
                .export(true)
                .optional(true)
                .services(ModuleDependency.Services.IMPORT)
                .addImportFilter(ModuleDependency.Filter.of("org/jboss/logging", true))
                .addExportFilter(ModuleDependency.Filter.of("org/jboss/logging/internal", false))
                .build();

        Assertions.assertEquals("org.jboss.logging", dep.name());
        Assertions.assertTrue(dep.isExport());
        Assertions.assertTrue(dep.isOptional());
        Assertions.assertTrue(dep.services().isPresent());
        Assertions.assertEquals(ModuleDependency.Services.IMPORT, dep.services().get());
        Assertions.assertEquals(1, dep.imports().size());
        Assertions.assertEquals(1, dep.exports().size());
    }

    @Test
    void servicesEnumToString() {
        Assertions.assertEquals("none", ModuleDependency.Services.NONE.toString());
        Assertions.assertEquals("import", ModuleDependency.Services.IMPORT.toString());
        Assertions.assertEquals("export", ModuleDependency.Services.EXPORT.toString());
    }

    @Test
    void filterCreation() {
        final ModuleDependency.Filter includeFilter = ModuleDependency.Filter.of("org/jboss/logging", true);
        Assertions.assertEquals("org/jboss/logging", includeFilter.path());
        Assertions.assertTrue(includeFilter.include());

        final ModuleDependency.Filter excludeFilter = ModuleDependency.Filter.of("org/jboss/logging/internal", false);
        Assertions.assertEquals("org/jboss/logging/internal", excludeFilter.path());
        Assertions.assertFalse(excludeFilter.include());
    }

    @Test
    void filterComparable() {
        final ModuleDependency.Filter a = ModuleDependency.Filter.of("a/path", true);
        final ModuleDependency.Filter b = ModuleDependency.Filter.of("b/path", true);
        final ModuleDependency.Filter c = ModuleDependency.Filter.of("c/path", false);

        // Natural ordering by path
        Assertions.assertTrue(a.compareTo(b) < 0);
        Assertions.assertTrue(b.compareTo(a) > 0);
        Assertions.assertEquals(0, a.compareTo(a));

        // Use in sorted collection
        final Set<ModuleDependency.Filter> sorted = new TreeSet<>();
        sorted.add(c);
        sorted.add(a);
        sorted.add(b);

        final var list = sorted.stream().toList();
        Assertions.assertEquals(a, list.get(0));
        Assertions.assertEquals(b, list.get(1));
        Assertions.assertEquals(c, list.get(2));
    }

    @Test
    void toStringContainsRelevantInfo() {
        final ModuleDependency dep = ModuleDependency.builder("org.jboss.logging")
                .export(true)
                .optional(false)
                .build();

        final String str = dep.toString();
        Assertions.assertTrue(str.contains("org.jboss.logging"));
        Assertions.assertTrue(str.contains("export=true"));
        Assertions.assertTrue(str.contains("optional=false"));
    }
}

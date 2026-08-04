package io.kyle.javaguard.filter;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SimpleFilterTest {

    @Test
    public void globalWildcardCrossesDirectories() {
        SimpleFilter filter = new SimpleFilter().addExpr("*");
        assertTrue(filter.filtrate("BOOT-INF/classes/app/Main.class"));
        assertTrue(filter.filtrate("BOOT-INF/lib/dependency.jar"));
        assertTrue(filter.filtrate("org/springframework/boot/loader/Launcher.class"));
    }

    @Test
    public void bootClassesWildcardDoesNotMatchDependencies() {
        SimpleFilter filter = new SimpleFilter().addExpr("BOOT-INF/classes/*");
        assertTrue(filter.filtrate("BOOT-INF/classes/app/Main.class"));
        assertTrue(filter.filtrate("BOOT-INF/classes/compat/data/fixture.json"));
        assertFalse(filter.filtrate("BOOT-INF/lib/dependency.jar"));
    }

    @Test
    public void questionMarkAndMultipleRulesAreSupported() {
        SimpleFilter filter = new SimpleFilter().addExpr("app/?.class").addExpr("config/*");
        assertTrue(filter.filtrate("app/A.class"));
        assertFalse(filter.filtrate("app/Main.class"));
        assertTrue(filter.filtrate("config/nested/value.json"));
    }

    @Test
    public void rawRegexIsSupported() {
        SimpleFilter filter = new SimpleFilter().addExpr("r:BOOT-INF/(classes|lib)/.*");
        assertTrue(filter.filtrate("BOOT-INF/classes/Main.class"));
        assertTrue(filter.filtrate("BOOT-INF/lib/a.jar"));
        assertFalse(filter.filtrate("META-INF/MANIFEST.MF"));
    }
}

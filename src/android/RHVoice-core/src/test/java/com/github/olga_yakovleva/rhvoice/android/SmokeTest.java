package com.github.olga_yakovleva.rhvoice.android;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/**
 * Test-kanarek: dowodzi, ze infrastruktura testow JVM w tym modulu dziala.
 * Nie testuje logiki aplikacji - jego jedynym zadaniem jest wykryc, gdy
 * uruchamianie testow przestanie dzialac.
 */
public class SmokeTest {
    @Test
    public void junitRuns() {
        assertThat(1 + 1).isEqualTo(2);
    }
}

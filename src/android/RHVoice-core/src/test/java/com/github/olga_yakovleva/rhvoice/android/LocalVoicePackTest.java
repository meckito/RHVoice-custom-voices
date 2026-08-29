package com.github.olga_yakovleva.rhvoice.android;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

/**
 * Testy reprezentacji lokalnego glosu i formatu jego wpisu w rejestrze.
 *
 * Format wpisu (jedna linia pliku local-voices.properties):
 * {@code <id> = <name>|<language>|<enabled>}
 */
public class LocalVoicePackTest {

    @Test
    public void buildsPackFromVoiceInfo() throws Exception {
        LocalVoiceInfo info = LocalVoiceInfo.parse(new java.io.ByteArrayInputStream(
                "name=Kazek-Beta\nlanguage=Polish\nformat=4\nrevision=0\n".getBytes("utf-8")));
        LocalVoicePack pack = LocalVoicePack.fromInfo(info);
        assertThat(pack.getId()).isEqualTo("kazek_beta");
        assertThat(pack.getName()).isEqualTo("Kazek-Beta");
        assertThat(pack.getLanguage()).isEqualTo("Polish");
        assertThat(pack.isEnabled()).isTrue();
    }

    @Test
    public void newVoiceIsEnabledByDefault() {
        assertThat(new LocalVoicePack("kazek_beta", "Kazek-Beta", "Polish", true).isEnabled()).isTrue();
    }

    @Test
    public void serializesToRegistryValue() {
        LocalVoicePack pack = new LocalVoicePack("kazek_beta", "Kazek-Beta", "Polish", true);
        assertThat(pack.toRegistryValue()).isEqualTo("Kazek-Beta|Polish|true");
    }

    @Test
    public void serializesDisabledVoice() {
        LocalVoicePack pack = new LocalVoicePack("kazek_beta", "Kazek-Beta", "Polish", false);
        assertThat(pack.toRegistryValue()).isEqualTo("Kazek-Beta|Polish|false");
    }

    @Test
    public void roundTripsThroughRegistryValue() {
        LocalVoicePack original = new LocalVoicePack("kazek_beta", "Kazek-Beta", "Polish", false);
        LocalVoicePack restored = LocalVoicePack.fromRegistryValue("kazek_beta", original.toRegistryValue());
        assertThat(restored.getId()).isEqualTo("kazek_beta");
        assertThat(restored.getName()).isEqualTo("Kazek-Beta");
        assertThat(restored.getLanguage()).isEqualTo("Polish");
        assertThat(restored.isEnabled()).isFalse();
    }

    @Test
    public void treatsUnknownEnabledFlagAsEnabled() {
        // Rejestr moze byc recznie zepsuty; brak jednoznacznego "false"
        // nie powinien po cichu wylaczac glosu uzytkownika.
        LocalVoicePack pack = LocalVoicePack.fromRegistryValue("kazek_beta", "Kazek-Beta|Polish|cokolwiek");
        assertThat(pack.isEnabled()).isTrue();
    }

    @Test
    public void toleratesMissingEnabledField() {
        LocalVoicePack pack = LocalVoicePack.fromRegistryValue("kazek_beta", "Kazek-Beta|Polish");
        assertThat(pack.isEnabled()).isTrue();
    }

    @Test
    public void rejectsRegistryValueWithoutLanguage() {
        try {
            LocalVoicePack.fromRegistryValue("kazek_beta", "Kazek-Beta");
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // tak ma byc
        }
    }

    @Test
    public void rejectsBlankId() {
        try {
            new LocalVoicePack("  ", "Kazek-Beta", "Polish", true);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // tak ma byc
        }
    }

    @Test
    public void rejectsNameContainingFieldSeparator() {
        // Separator w nazwie rozjechalby rejestr, wiec musi byc odrzucony
        // przy tworzeniu, a nie dopiero przy odczycie.
        try {
            new LocalVoicePack("kazek_beta", "Kazek|Beta", "Polish", true);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // tak ma byc
        }
    }

    @Test
    public void installationDirectoryIsNamedAfterId() {
        File root = new File("/tmp/local-voices");
        LocalVoicePack pack = new LocalVoicePack("kazek_beta", "Kazek-Beta", "Polish", true);
        assertThat(pack.getDir(root)).isEqualTo(new File(root, "kazek_beta"));
    }

    @Test
    public void equalPacksAreEqualAndShareHashCode() {
        LocalVoicePack a = new LocalVoicePack("kazek_beta", "Kazek-Beta", "Polish", true);
        LocalVoicePack b = new LocalVoicePack("kazek_beta", "Kazek-Beta", "Polish", true);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}

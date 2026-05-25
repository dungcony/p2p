package dungcony.ds;

import dungcony.ds.config.RuntimeOption;
import dungcony.ds.config.RuntimeOptionParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppRuntimeOptionsTest {

    @Test
    void resolvesInlineRuntimeOptions() {
        RuntimeOption options = resolve("--data-dir=tmp/profile-data", "--peer-port=15001");

        assertEquals(Path.of("tmp/profile-data").normalize(), options.dataRoot());
        assertNull(options.profileId());
        assertNull(options.peerName());
        assertEquals(15001, options.peerPort());
    }

    @Test
    void resolvesSeparatedRuntimeOptions() {
        RuntimeOption options = resolve("--data-dir", "tmp/profile-data", "--port", "15002");

        assertEquals(Path.of("tmp/profile-data").normalize(), options.dataRoot());
        assertEquals(15002, options.peerPort());
    }

    @Test
    void keepsPreviousValuesWhenLaterValuesAreInvalid() {
        RuntimeOption options = resolve(
                "--data-dir=tmp/profile-data",
                "--peer-port=15003",
                "--data-dir=",
                "--peer-port=abc",
                "--port=70000"
        );

        assertEquals(Path.of("tmp/profile-data").normalize(), options.dataRoot());
        assertEquals(15003, options.peerPort());
    }

    @Test
    void missingDataDirValueDoesNotConsumeNextOption() {
        RuntimeOption options = resolve("--data-dir", "--peer-port", "15004");

        assertEquals(Path.of("peer-node", "src", "main", "resources", "data"), options.dataRoot());
        assertEquals(15004, options.peerPort());
    }

    @Test
    void resolvesProfileRuntimeOption() {
        RuntimeOption options = resolve("--profile=alice");

        assertEquals("alice", options.profileId());
        assertNull(options.peerName());
    }

    @Test
    void resolvesPeerNameForGeneratedProfile() {
        RuntimeOption options = resolve("--peer-name=Bob");

        assertNull(options.profileId());
        assertEquals("Bob", options.peerName());
        assertNull(options.peerPort());
    }

    private static RuntimeOption resolve(String... args) {
        return RuntimeOptionParser.resolveRuntimeOptions(args);
    }
}

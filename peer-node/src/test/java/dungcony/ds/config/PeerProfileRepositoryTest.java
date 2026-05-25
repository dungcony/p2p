package dungcony.ds.config;

import dungcony.ds.repositories.PeerProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeerProfileRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void createsProfileWithGeneratedIdAndFindsItByName() {
        PeerProfileRepository repository = new PeerProfileRepository();
        PeerProfile profile = repository.createNewWithName(tempDir, "Alice");
        repository.save(profile);

        Optional<PeerProfile> loaded = repository.findByName(tempDir, "alice");

        assertTrue(loaded.isPresent());
        assertEquals("Alice", loaded.get().getPeerName());
        assertFalse(loaded.get().getPeerId().isBlank());
        assertNotEquals("Alice", loaded.get().getPeerId());
    }

    @Test
    void detectsExistingGeneratedPeerId() {
        PeerProfileRepository repository = new PeerProfileRepository();
        PeerProfile profile = repository.createNewWithName(tempDir, "Alice");
        repository.save(profile);

        assertTrue(repository.existsByPeerId(tempDir, profile.getPeerId()));
        assertFalse(repository.existsByPeerId(tempDir, "missing-id"));
    }
}

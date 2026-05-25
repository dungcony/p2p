package dungcony.ds.services.impl.profile;

import dungcony.ds.config.PeerProfile;
import dungcony.ds.dtos.ProfileSelection;
import dungcony.ds.repositories.PeerProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileSelectionImplTest {

    @TempDir
    Path tempDir;

    @Test
    void ignoresDemoProfilesAndCreatesEditableUserProfile() {
        PeerProfileRepository repository = new PeerProfileRepository();
        repository.save(new PeerProfile("alice", "Alice", 5001, "localhost", 9000, tempDir));
        repository.save(new PeerProfile("bob", "Bob", 5002, "localhost", 9000, tempDir));
        repository.save(new PeerProfile("carol", "Carol", 5003, "localhost", 9000, tempDir));

        ProfileSelection selection = new ProfileSelectionImpl(tempDir, repository).selectProfile();

        assertTrue(selection.newProfile());
        assertTrue(selection.editBeforeStart());
        assertFalse("alice".equalsIgnoreCase(selection.config().getPeerId()));
        assertFalse("bob".equalsIgnoreCase(selection.config().getPeerId()));
        assertFalse("carol".equalsIgnoreCase(selection.config().getPeerId()));
    }

    @Test
    void startsExistingUserProfileDirectly() {
        PeerProfileRepository repository = new PeerProfileRepository();
        repository.save(new PeerProfile("alice", "Alice", 5001, "localhost", 9000, tempDir));
        repository.save(repository.createNewWithName(tempDir, "Dung"));

        ProfileSelection selection = new ProfileSelectionImpl(tempDir, repository).selectProfile();

        assertFalse(selection.newProfile());
        assertFalse(selection.editBeforeStart());
        assertEquals("Dung", selection.config().getPeerName());
    }
}

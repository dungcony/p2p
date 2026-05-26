package dungcony.ds.services.impl.group;

import dungcony.ds.model.Group;
import dungcony.ds.repositories.GroupRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GroupManagerTest {

    @Test
    void keepsLocalGroupsWhenBootstrapReturnsEmptyList() {
        InMemoryGroupRepository repository = new InMemoryGroupRepository();
        repository.save(new Group("legacy-group", "hihi", List.of()));
        GroupManager groupManager = new GroupManager(repository, null);

        groupManager.replaceAll(List.of());

        assertNotNull(groupManager.getGroup("legacy-group"));
        assertEquals(1, groupManager.getAllGroups().size());
        assertEquals(1, repository.findAll().size());
    }

    private static class InMemoryGroupRepository implements GroupRepository {
        private final List<Group> groups = new ArrayList<>();

        @Override
        public void save(Group group) {
            groups.removeIf(existing -> existing.getGroupId().equals(group.getGroupId()));
            groups.add(group);
        }

        @Override
        public void saveAll(Collection<Group> groups) {
            this.groups.clear();
            this.groups.addAll(groups);
        }

        @Override
        public List<Group> findAll() {
            return new ArrayList<>(groups);
        }
    }
}

package dungcony.ds.repositories;

import dungcony.ds.model.Group;

import java.util.Collection;
import java.util.List;

/**
 * Repository lưu và đọc group local
 */
public interface GroupRepository {
    void save(Group group);

    void saveAll(Collection<Group> groups);

    List<Group> findAll();
}

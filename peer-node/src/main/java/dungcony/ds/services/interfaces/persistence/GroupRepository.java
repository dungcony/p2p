package dungcony.ds.services.interfaces.persistence;

import dungcony.ds.model.Group;

import java.util.Collection;
import java.util.List;

public interface GroupRepository {
    void save(Group group);

    void saveAll(Collection<Group> groups);

    List<Group> findAll();
}

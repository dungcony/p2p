package dungcony.ds.repositories;

import lombok.extern.slf4j.Slf4j;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dungcony.ds.model.Group;
import dungcony.ds.repositories.GroupRepository;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Repository JSON local lưu danh sách group của profile hiện tại
 */
@Slf4j
public class LocalGroupRepo implements GroupRepository {
private static final Type GROUP_LIST_TYPE = new TypeToken<List<Group>>() {
    }.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path groupFilePath;

    // Khởi tạo local JSON store cho group trong dataDir của profile hiện tại
    public LocalGroupRepo(Path dataDir) {
        Path resolvedDataDir = dataDir == null
                ? Path.of("peer-node", "src", "main", "resources", "data")
                : dataDir.normalize();
        this.groupFilePath = resolvedDataDir.resolve("groups.json");
        initializeStorage();
    }

    // Tạo file groups.json nếu profile chưa có group store
    private void initializeStorage() {
        try {
            Files.createDirectories(groupFilePath.getParent());
            if (!Files.exists(groupFilePath)) {
                Files.writeString(groupFilePath, "[]", StandardCharsets.UTF_8);
            }
            log.info("Kho JSON nhóm local đã sẵn sàng. path={}", groupFilePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Không thể khởi tạo kho JSON nhóm local: {}", e.getMessage());
        }
    }

    // Lưu hoặc cập nhật một group vào groups.json
    @Override
    public synchronized void save(Group group) {
        if (group == null) {
            log.warn("LocalGroupRepo bỏ qua lưu nhóm null.");
            return;
        }
        List<Group> groups = findAll();
        groups.removeIf(existingGroup -> group.getGroupId().equals(existingGroup.getGroupId()));
        groups.add(group);
        saveAll(groups);
        log.info("Đã lưu nhóm local. groupId={}, tên={}", group.getGroupId(), group.getName());
    }

    // Ghi lại toàn bộ danh sách group của profile hiện tại
    @Override
    public synchronized void saveAll(Collection<Group> groups) {
        List<Group> sortedGroups = new ArrayList<>(groups == null ? List.of() : groups);
        sortedGroups.sort(Comparator.comparing(Group::getName).thenComparing(Group::getGroupId));
        try {
            Files.writeString(groupFilePath, gson.toJson(sortedGroups), StandardCharsets.UTF_8);
            log.debug("Đã ghi nhóm local. sốLượng={}", sortedGroups.size());
        } catch (IOException e) {
            log.error("Không thể ghi JSON nhóm local: {}", e.getMessage());
        }
    }

    // Đọc tất cả group mà profile hiện tại đang tham gia
    @Override
    public synchronized List<Group> findAll() {
        try {
            if (!Files.exists(groupFilePath)) {
                return new ArrayList<>();
            }
            String json = Files.readString(groupFilePath, StandardCharsets.UTF_8);
            if (json.isBlank()) {
                return new ArrayList<>();
            }
            List<Group> groups = gson.fromJson(json, GROUP_LIST_TYPE);
            List<Group> result = groups == null ? new ArrayList<>() : new ArrayList<>(groups);
            log.debug("Đã nạp nhóm local. sốLượng={}", result.size());
            return result;
        } catch (IOException | RuntimeException e) {
            log.error("Không thể đọc JSON nhóm local: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}

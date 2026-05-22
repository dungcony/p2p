package dungcony.ds.repositories;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dungcony.ds.model.Group;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class LocalGroupRepo {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalGroupRepo.class);
private static final Type GROUP_LIST_TYPE = new TypeToken<List<Group>>() {
    }.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path groupFilePath;

    // Khởi tạo local JSON store cho group trong dataDir của profile hiện tại.
    public LocalGroupRepo(Path dataDir) {
        Path resolvedDataDir = dataDir == null
                ? Path.of("peer-node", "src", "main", "resources", "data")
                : dataDir.normalize();
        this.groupFilePath = resolvedDataDir.resolve("groups.json");
        initializeStorage();
    }

    // Tạo file groups.json nếu profile chưa có group store.
    private void initializeStorage() {
        try {
            Files.createDirectories(groupFilePath.getParent());
            if (!Files.exists(groupFilePath)) {
                Files.writeString(groupFilePath, "[]", StandardCharsets.UTF_8);
            }
            LOGGER.info("Kho JSON nhóm local đã sẵn sàng. path=" + groupFilePath.toAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Không thể khởi tạo kho JSON nhóm local: " + e.getMessage());
        }
    }

    // Lưu hoặc cập nhật một group vào groups.json.
    public synchronized void save(Group group) {
        if (group == null) {
            LOGGER.warn("LocalGroupRepo bỏ qua lưu nhóm null.");
            return;
        }
        List<Group> groups = findAll();
        groups.removeIf(existingGroup -> group.getGroupId().equals(existingGroup.getGroupId()));
        groups.add(group);
        saveAll(groups);
        LOGGER.info("Đã lưu nhóm local. groupId=" + group.getGroupId()
                + ", tên=" + group.getName());
    }

    // Ghi lại toàn bộ danh sách group của profile hiện tại.
    public synchronized void saveAll(Collection<Group> groups) {
        List<Group> sortedGroups = new ArrayList<>(groups == null ? List.of() : groups);
        sortedGroups.sort(Comparator.comparing(Group::getName).thenComparing(Group::getGroupId));
        try {
            Files.writeString(groupFilePath, gson.toJson(sortedGroups), StandardCharsets.UTF_8);
            LOGGER.debug("Đã ghi nhóm local. sốLượng=" + sortedGroups.size());
        } catch (IOException e) {
            LOGGER.error("Không thể ghi JSON nhóm local: " + e.getMessage());
        }
    }

    // Đọc tất cả group mà profile hiện tại đang tham gia.
    public synchronized List<Group> findAll() {
        try {
            if (!Files.exists(groupFilePath)) {
                return new ArrayList<>();
            }
            String json = Files.readString(groupFilePath, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }
            List<Group> groups = gson.fromJson(json, GROUP_LIST_TYPE);
            List<Group> result = groups == null ? new ArrayList<>() : new ArrayList<>(groups);
            LOGGER.debug("Đã nạp nhóm local. sốLượng=" + result.size());
            return result;
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Không thể đọc JSON nhóm local: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}

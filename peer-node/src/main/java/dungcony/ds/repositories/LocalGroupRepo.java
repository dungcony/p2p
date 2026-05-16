package dungcony.ds.repositories;

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
    private static final Type GROUP_LIST_TYPE = new TypeToken<List<Group>>() {
    }.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path groupFilePath;

    /**
     * Khoi tao local JSON store cho group trong dataDir cua profile hien tai.
     */
    public LocalGroupRepo(Path dataDir) {
        Path resolvedDataDir = dataDir == null
                ? Path.of("peer-node", "src", "main", "resources", "data")
                : dataDir.normalize();
        this.groupFilePath = resolvedDataDir.resolve("groups.json");
        initializeStorage();
    }

    /**
     * Tao file groups.json neu profile chua co group store.
     */
    private void initializeStorage() {
        try {
            Files.createDirectories(groupFilePath.getParent());
            if (!Files.exists(groupFilePath)) {
                Files.writeString(groupFilePath, "[]", StandardCharsets.UTF_8);
            }
            System.out.println("[INFO] Kho JSON nhóm local đã sẵn sàng. path=" + groupFilePath.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("[ERROR] Không thể khởi tạo kho JSON nhóm local: " + e.getMessage());
        }
    }

    /**
     * Luu hoac cap nhat mot group vao groups.json.
     */
    public synchronized void save(Group group) {
        if (group == null) {
            System.out.println("[WARN] LocalGroupRepo bỏ qua lưu nhóm null.");
            return;
        }
        List<Group> groups = findAll();
        groups.removeIf(existingGroup -> group.getGroupId().equals(existingGroup.getGroupId()));
        groups.add(group);
        saveAll(groups);
        System.out.println("[INFO] Đã lưu nhóm local. groupId=" + group.getGroupId()
                + ", tên=" + group.getName());
    }

    /**
     * Ghi lai toan bo danh sach group cua profile hien tai.
     */
    public synchronized void saveAll(Collection<Group> groups) {
        List<Group> sortedGroups = new ArrayList<>(groups == null ? List.of() : groups);
        sortedGroups.sort(Comparator.comparing(Group::getName).thenComparing(Group::getGroupId));
        try {
            Files.writeString(groupFilePath, gson.toJson(sortedGroups), StandardCharsets.UTF_8);
            System.out.println("[DEBUG] Đã ghi nhóm local. sốLượng=" + sortedGroups.size());
        } catch (IOException e) {
            System.out.println("[ERROR] Không thể ghi JSON nhóm local: " + e.getMessage());
        }
    }

    /**
     * Doc tat ca group ma profile hien tai dang tham gia.
     */
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
            System.out.println("[DEBUG] Đã nạp nhóm local. sốLượng=" + result.size());
            return result;
        } catch (IOException | RuntimeException e) {
            System.out.println("[ERROR] Không thể đọc JSON nhóm local: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}

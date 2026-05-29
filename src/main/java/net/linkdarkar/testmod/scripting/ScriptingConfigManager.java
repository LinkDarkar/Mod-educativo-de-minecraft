package net.linkdarkar.testmod.scripting;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ScriptingConfigManager {
    private static final ScriptingConfigManager INSTANCE = new ScriptingConfigManager();

    private final HashMap<UUID, ScriptingConfig> configs = new HashMap<>();
    private final HashMap<UUID, EntityActions> actionsMap = new HashMap<>();

    public static ScriptingConfigManager getInstance() { return INSTANCE; }

    public ScriptingConfig getConfig(UUID entityUuid) {
        return configs.computeIfAbsent(entityUuid, k -> new ScriptingConfig());
    }

    public EntityActions getActions(UUID entityUuid) {
        return actionsMap.computeIfAbsent(entityUuid, k -> new EntityActions());
    }

    public static class PersistentVariable {
        public String name = "var";
        public String value = "0";

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("name", name);
            nbt.putString("val", value);
            return nbt;
        }

        public void loadNbt(NbtCompound nbt) {
            this.name = nbt.getString("name");
            this.value = nbt.getString("val");
        }
    }

    public static class CheckpointData {
        public double x;
        public double y;
        public double z;
        public double radius = 2.0;
        public String entityUuid = "";
        public boolean useEntity = false;

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putDouble("x", x);
            nbt.putDouble("y", y);
            nbt.putDouble("z", z);
            nbt.putDouble("radius", radius);
            nbt.putString("uuid", entityUuid);
            nbt.putBoolean("useEntity", useEntity);
            return nbt;
        }

        public void loadNbt(NbtCompound nbt) {
            this.x = nbt.getDouble("x");
            this.y = nbt.getDouble("y");
            this.z = nbt.getDouble("z");
            this.radius = nbt.contains("radius") ? nbt.getDouble("radius") : 2.0;
            this.entityUuid = nbt.getString("uuid");
            this.useEntity = nbt.getBoolean("useEntity");
        }
    }

    public static class TestCase {
        public List<PersistentVariable> variables = new ArrayList<>();

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            NbtList list = new NbtList();
            for (PersistentVariable pv : variables) list.add(pv.toNbt());
            nbt.put("vars", list);
            return nbt;
        }

        public void loadNbt(NbtCompound nbt) {
            variables.clear();
            NbtList list = nbt.getList("vars", 10);
            for (int i = 0; i < list.size(); i++) {
                PersistentVariable pv = new PersistentVariable();
                pv.loadNbt(list.getCompound(i));
                variables.add(pv);
            }
        }
    }

    public static class ScriptingConfig {
        public boolean allowVar = true;
        public boolean allowVarF = false;
        public boolean allowIf = true;
        public boolean allowElse = true;
        public boolean allowWhile = true;
        public boolean allowPrint = true;
        public boolean allowLookAt = true;
        public boolean allowFollow = true;
        public boolean allowWalkForward = true;
        public boolean allowDistanceCheck = true;
        public boolean allowPlace = false;
        public boolean allowCommand = false;

        public List<PersistentVariable> persistentVariables = new ArrayList<>();

        public List<TestCase> testCases = new ArrayList<>();

        public List<CheckpointData> checkpoints = new ArrayList<>();

        public List<PersistentVariable> userTestVariables = new ArrayList<>();

    }

    public static class ActionEventData {
        public String commands = "";
        public int maxExecutions = 0; // 0 means infinite
        public int currentExecutions = 0;

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("cmds", commands);
            nbt.putInt("max", maxExecutions);
            nbt.putInt("count", currentExecutions);
            return nbt;
        }

        public void loadNbt(NbtCompound nbt) {
            this.commands = nbt.getString("cmds");
            this.maxExecutions = nbt.getInt("max");
            this.currentExecutions = nbt.getInt("count");
        }
    }

    public NbtCompound exportAllToNbt(UUID entityUuid) {
        NbtCompound nbt = new NbtCompound();

        // Save Configs
        ScriptingConfig config = getConfig(entityUuid);
        NbtCompound configNbt = new NbtCompound();
        configNbt.putBoolean("var", config.allowVar);
        configNbt.putBoolean("varF", config.allowVarF);
        configNbt.putBoolean("if", config.allowIf);
        configNbt.putBoolean("else", config.allowElse);
        configNbt.putBoolean("while", config.allowWhile);
        configNbt.putBoolean("print", config.allowPrint);
        configNbt.putBoolean("lookAt", config.allowLookAt);
        configNbt.putBoolean("walkForward", config.allowWalkForward);
        configNbt.putBoolean("follow", config.allowFollow);
        configNbt.putBoolean("dist", config.allowDistanceCheck);
        configNbt.putBoolean("place", config.allowPlace);
        configNbt.putBoolean("cmd", config.allowCommand);

        // Save Persistent Variables
        NbtList pvList = new NbtList();
        for (PersistentVariable pv : config.persistentVariables) pvList.add(pv.toNbt());
        configNbt.put("persistentVars", pvList);

        // Save Test Cases
        NbtList tcList = new NbtList();
        for (TestCase tc : config.testCases) tcList.add(tc.toNbt());
        configNbt.put("testCases", tcList);

        NbtList utvList = new NbtList();
        for (PersistentVariable pv : config.userTestVariables) utvList.add(pv.toNbt());
        configNbt.put("userTestVars", utvList);

        // Save Checkpoints
        NbtList cpList = new NbtList();
        for (CheckpointData cp : config.checkpoints) cpList.add(cp.toNbt());
        configNbt.put("checkpoints", cpList);

        nbt.put("config", configNbt);

        // Save Actions
        nbt.put("actions", getActions(entityUuid).toNbt());

        return nbt;
    }

    public void importAllFromNbt(UUID entityUuid, NbtCompound nbt) {
        if (nbt.contains("config")) {
            NbtCompound c = nbt.getCompound("config");
            ScriptingConfig config = getConfig(entityUuid);
            config.allowVar = c.getBoolean("var");
            config.allowVarF = c.getBoolean("varF");
            config.allowIf = c.getBoolean("if");
            config.allowElse = c.getBoolean("else");
            config.allowWhile = c.getBoolean("while");
            config.allowPrint = c.getBoolean("print");
            config.allowLookAt = c.getBoolean("lookAt");
            config.allowWalkForward = c.getBoolean("walkForward");
            config.allowFollow = c.getBoolean("follow");
            config.allowDistanceCheck = c.getBoolean("dist");
            config.allowPlace = c.getBoolean("place");
            config.allowCommand = c.getBoolean("cmd");

            config.persistentVariables.clear();
            if (c.contains("persistentVars")) {
                NbtList pvList = c.getList("persistentVars", 10);
                for (int i = 0; i < pvList.size(); i++) {
                    PersistentVariable pv = new PersistentVariable();
                    pv.loadNbt(pvList.getCompound(i));
                    config.persistentVariables.add(pv);
                }
            }

            config.testCases.clear();
            if (c.contains("testCases")) {
                NbtList tcList = c.getList("testCases", 10);
                for (int i = 0; i < tcList.size(); i++) {
                    TestCase tc = new TestCase();
                    tc.loadNbt(tcList.getCompound(i));
                    config.testCases.add(tc);
                }
            }

            config.userTestVariables.clear();
            if (c.contains("userTestVars")) {
                NbtList utvList = c.getList("userTestVars", 10);
                for (int i = 0; i < utvList.size(); i++) {
                    PersistentVariable pv = new PersistentVariable();
                    pv.loadNbt(utvList.getCompound(i));
                    config.userTestVariables.add(pv);
                }
            }

            config.checkpoints.clear();
            if (c.contains("checkpoints")) {
                NbtList cpList = c.getList("checkpoints", 10);
                for (int i = 0; i < cpList.size(); i++) {
                    CheckpointData cp = new CheckpointData();
                    cp.loadNbt(cpList.getCompound(i));
                    config.checkpoints.add(cp);
                }
            }
        }
        if (nbt.contains("actions")) {
            getActions(entityUuid).loadNbt(nbt.getCompound("actions"));
        }
    }

    public static class EntityActions {
        public ActionEventData anyExecute = new ActionEventData();
        public ActionEventData executeCorrect = new ActionEventData();
        public ActionEventData executeWrong = new ActionEventData();

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.put("any", anyExecute.toNbt());
            nbt.put("correct", executeCorrect.toNbt());
            nbt.put("wrong", executeWrong.toNbt());
            return nbt;
        }

        public void loadNbt(NbtCompound nbt) {
            if (nbt.contains("any")) anyExecute.loadNbt(nbt.getCompound("any"));
            if (nbt.contains("correct")) executeCorrect.loadNbt(nbt.getCompound("correct"));
            if (nbt.contains("wrong")) executeWrong.loadNbt(nbt.getCompound("wrong"));
        }
    }

    public java.util.Set<UUID> getAllTrackedUuids() {
        java.util.HashSet<UUID> allUuids = new java.util.HashSet<>(configs.keySet());
        allUuids.addAll(actionsMap.keySet());
        return allUuids;
    }

    public void syncUserTestVariables(UUID entityUuid) {
        ScriptingConfig config = getConfig(entityUuid);
        if (config.testCases.isEmpty()) {
            config.userTestVariables.clear();
            return;
        }

        TestCase templateCase = config.testCases.get(0);

        // Remove any variables the user has that no longer exist in the template
        config.userTestVariables.removeIf(userVar ->
                templateCase.variables.stream().noneMatch(templateVar -> templateVar.name.equals(userVar.name))
        );

        // Add any missing variables from the template to the user's list
        for (PersistentVariable templateVar : templateCase.variables) {
            boolean exists = config.userTestVariables.stream().anyMatch(userVar -> userVar.name.equals(templateVar.name));
            if (!exists) {
                PersistentVariable newVar = new PersistentVariable();
                newVar.name = templateVar.name;
                newVar.value = templateVar.value;
                config.userTestVariables.add(newVar);
            }
        }
    }

    // Forces all Test Cases to share the exact same variable names as Case 1
    public void ensureTestCaseSync(UUID entityUuid) {
        ScriptingConfig config = getConfig(entityUuid);
        if (config.testCases.isEmpty()) return;

        TestCase master = config.testCases.get(0);

        for (int i = 1; i < config.testCases.size(); i++) {
            TestCase current = config.testCases.get(i);
            List<PersistentVariable> newVars = new ArrayList<>();

            for (int v = 0; v < master.variables.size(); v++) {
                PersistentVariable masterVar = master.variables.get(v);
                PersistentVariable existing = null;

                // Try to find by index first (fastest for identical ordering)
                if (v < current.variables.size()) {
                    existing = current.variables.get(v);
                }

                // If names don't match, search by name (handles legacy backwards-compatibility)
                if (existing == null || !existing.name.equals(masterVar.name)) {
                    existing = current.variables.stream().filter(pv -> pv.name.equals(masterVar.name)).findFirst().orElse(null);
                }

                PersistentVariable toAdd = new PersistentVariable();
                toAdd.name = masterVar.name; // Force name to match master
                toAdd.value = existing != null ? existing.value : masterVar.value; // Keep existing value, or default to master's value
                newVars.add(toAdd);
            }
            current.variables = newVars;
        }
    }

    // Clear data when switching worlds
    public void clear() {
        configs.clear();
        actionsMap.clear();
    }

    public void markDirty() {
        ScriptActorManager.getInstance().markDirty();
    }
}
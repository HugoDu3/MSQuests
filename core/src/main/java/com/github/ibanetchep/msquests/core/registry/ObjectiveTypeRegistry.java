package com.github.ibanetchep.msquests.core.registry;

import com.github.ibanetchep.msquests.core.quest.QuestObjective;
import com.github.ibanetchep.msquests.core.quest.QuestObjectiveConfig;
import com.github.ibanetchep.msquests.core.quest.QuestObjectiveHandler;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for managing objective types and their associated classes.
 * Provides functionality to register objective types and instantiate objectives.
 */
public class ObjectiveTypeRegistry {

    private final Map<String, ObjectiveTypeEntry> registeredTypes = new HashMap<>();
    private final Map<String, QuestObjectiveHandler<?>> handlers = new HashMap<>();

    public ObjectiveTypeRegistry() {
    }

    /**
     * Registers a new objective type with its associated config and handler.
     *
     * @param type the objective type key
     * @param configClass the objective config class
     * @param objectiveClass the corresponding objective class
     * @param handler the handler for this objective type
     * @throws IllegalArgumentException if the config class is not annotated with ObjectiveType
     */
    public <D extends QuestObjectiveConfig, O extends QuestObjective<D>> void registerType(
            String type,
            Class<D> configClass,
            Class<O> objectiveClass,
            QuestObjectiveHandler<O> handler
    ) {
        registeredTypes.put(type, new ObjectiveTypeEntry(configClass, objectiveClass));
        handlers.put(type, handler);
    }


    /**
     * Gets the objective config class by its type name.
     *
     * @param name the objective type name
     * @return the objective config class, or null if not found
     */
    public Class<? extends QuestObjectiveConfig> getConfigClass(String name) {
        ObjectiveTypeEntry entry = registeredTypes.get(name);
        return entry != null ? entry.configClass() : null;
    }

    /**
     * Gets the objective class by its type name.
     *
     * @param name the objective type name
     * @return the objective class, or null if not found
     */
    public Class<? extends QuestObjective<?>> getObjectiveClass(String name) {
        ObjectiveTypeEntry entry = registeredTypes.get(name);
        return entry != null ? entry.objectiveClass() : null;
    }

    /**
     * Creates a new objective config instance from configuration.
     *
     * @param config the configuration map for the objective
     * @return the created objective config, or null if the type is not registered
     */
    @SuppressWarnings("unchecked")
    public QuestObjectiveConfig createConfig(Map<String, Object> config) {
        String type = (String) config.get("type");

        if (type == null) {
            throw new IllegalArgumentException("Configuration does not contain a type.");
        }

        try {
            Class<? extends QuestObjectiveConfig> configClass = getConfigClass(type);
            if (configClass == null) {
                return null;
            }

            Constructor<? extends QuestObjectiveConfig> constructor = configClass.getConstructor(Map.class);
            return constructor.newInstance(config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create objective config of type " + type, e);
        }
    }

    /**
     * Gets all registered objective types.
     *
     * @return a map of all registered objective types with their entries
     */
    public Map<String, ObjectiveTypeEntry> getRegisteredTypes() {
        return Map.copyOf(registeredTypes);
    }

    public QuestObjectiveHandler<?> getHandler(String type) {
        return handlers.get(type);
    }

    /**
     * Internal class representing an entry in the objective type registry.
     * Contains both the config class and the objective class.
     */
    public record ObjectiveTypeEntry(
            Class<? extends QuestObjectiveConfig> configClass,
            Class<? extends QuestObjective<?>> objectiveClass
    ) {}
}
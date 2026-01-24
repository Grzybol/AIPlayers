package pl.nop.aiplayers.ai.controller;

import pl.nop.aiplayers.model.AIControllerType;

import java.util.EnumMap;
import java.util.Map;

public class AIControllerRegistry {

    private final Map<AIControllerType, AIController> controllers = new EnumMap<>(AIControllerType.class);

    public void registerDefaults(DummyAIController dummy) {
        controllers.put(AIControllerType.DUMMY, dummy);
    }

    public void register(AIControllerType type, AIController controller) {
        controllers.put(type, controller);
    }

    public AIController getController(AIControllerType type) {
        return controllers.getOrDefault(type, controllers.get(AIControllerType.DUMMY));
    }
}

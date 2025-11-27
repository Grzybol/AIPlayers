package pl.nop.aiplayers.ai.controller;

import pl.nop.aiplayers.model.AIControllerType;

import java.util.EnumMap;
import java.util.Map;

public class AIControllerRegistry {

    private final Map<AIControllerType, AIController> controllers = new EnumMap<>(AIControllerType.class);

    public void registerDefaults(DummyAIController dummy, HttpAIController http, OpenAIAIController openai) {
        controllers.put(AIControllerType.DUMMY, dummy);
        controllers.put(AIControllerType.HTTP, http);
        controllers.put(AIControllerType.OPENAI, openai);
    }

    public void register(AIControllerType type, AIController controller) {
        controllers.put(type, controller);
    }

    public AIController getController(AIControllerType type) {
        return controllers.getOrDefault(type, controllers.get(AIControllerType.DUMMY));
    }
}

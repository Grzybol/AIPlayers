package pl.nop.aiplayers.logging;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import pl.nop.aiplayers.AIPlayersPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

final class ElasticBufferBridge {

    private static final String ELASTIC_BUFFER_PLUGIN_NAME = "ElasticBuffer";
    private static final String ELASTIC_BUFFER_API_CLASS = "org.betterbox.elasticBuffer.ElasticBufferAPI";

    private final AIPlayersPlugin plugin;
    private boolean enabled;
    private Object api;
    private Method logMethod;

    private ElasticBufferBridge(AIPlayersPlugin plugin) {
        this.plugin = plugin;
    }

    static ElasticBufferBridge create(AIPlayersPlugin plugin) {
        ElasticBufferBridge bridge = new ElasticBufferBridge(plugin);
        bridge.initialize();
        return bridge;
    }

    void log(String level, String message) {
        if (!enabled) {
            return;
        }
        try {
            String pluginName = plugin.getDescription().getName();
            int paramCount = logMethod.getParameterCount();
            if (paramCount == 4) {
                logMethod.invoke(api, message, level, pluginName, null);
            } else if (paramCount == 6) {
                logMethod.invoke(api, message, level, pluginName, null, null, null);
            } else if (paramCount == 7) {
                logMethod.invoke(api, message, level, pluginName, null, null, null, null);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            plugin.getLogger().warning("Failed to forward log to ElasticBuffer: " + e.getMessage());
        }
    }

    private void initialize() {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        Plugin elasticPlugin = pluginManager.getPlugin(ELASTIC_BUFFER_PLUGIN_NAME);
        if (elasticPlugin == null || !elasticPlugin.isEnabled()) {
            plugin.getLogger().warning("ElasticBuffer plugin not found; logs will only be written to file.");
            return;
        }
        try {
            Class<?> apiClass = Class.forName(ELASTIC_BUFFER_API_CLASS);
            Constructor<?> constructor = findConstructor(apiClass, elasticPlugin);
            if (constructor == null) {
                plugin.getLogger().warning("ElasticBuffer API constructor not found; logs will only be written to file.");
                return;
            }
            this.api = constructor.newInstance(elasticPlugin);
            this.logMethod = resolveLogMethod(apiClass);
            if (logMethod == null) {
                plugin.getLogger().warning("ElasticBuffer API log method not found; logs will only be written to file.");
                return;
            }
            this.enabled = true;
            plugin.getLogger().info("ElasticBuffer detected; AIPlayers logs will be forwarded.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("ElasticBuffer API class not available; logs will only be written to file.");
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            plugin.getLogger().warning("Failed to initialize ElasticBuffer integration: " + e.getMessage());
        }
    }

    private Constructor<?> findConstructor(Class<?> apiClass, Plugin elasticPlugin) {
        for (Constructor<?> constructor : apiClass.getConstructors()) {
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length == 1 && params[0].isAssignableFrom(elasticPlugin.getClass())) {
                return constructor;
            }
        }
        return null;
    }

    private Method resolveLogMethod(Class<?> apiClass) {
        Method fallbackSeven = null;
        for (Method method : apiClass.getMethods()) {
            if (!Objects.equals(method.getName(), "log")) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params.length == 4 && allStrings(params)) {
                return method;
            }
            if (params.length == 6 && allStrings(params)) {
                return method;
            }
            if (params.length == 7 && allStrings(params, 6) && Map.class.isAssignableFrom(params[6])) {
                fallbackSeven = method;
            }
        }
        return fallbackSeven;
    }

    private boolean allStrings(Class<?>[] params) {
        return allStrings(params, params.length);
    }

    private boolean allStrings(Class<?>[] params, int length) {
        for (int i = 0; i < length; i++) {
            if (!String.class.isAssignableFrom(params[i])) {
                return false;
            }
        }
        return true;
    }
}

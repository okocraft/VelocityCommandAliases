package net.okocraft.velocitycommandaliases.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.okocraft.velocitycommandaliases.Main;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class CommandConfig extends CustomConfig {

    public CommandConfig(Main plugin) {
        super(plugin, "commands.yml");
    }

    public List<String> getChildren(String alias) {
        return getAliasesMap().getOrDefault(alias.toLowerCase(Locale.ROOT), Collections.emptyList());
    }

    public Map<String, List<String>> getAliasesMap() {
        Map<String, List<String>> result = new HashMap<>();

        ConfigurationNode aliases = get().node("aliases");

        for (Object key : aliases.childrenMap().keySet()) {
            String alias = String.valueOf(key);
            List<String> children = this.getChildren(aliases.node(key));

            if (!children.isEmpty()) {
                result.put(alias.toLowerCase(Locale.ROOT), Collections.unmodifiableList(children));
            }
        }

        return result;
    }

    private List<String> getChildren(ConfigurationNode node) {
        try {
            return node.getList(String.class);
        } catch (SerializationException ignored) {
        }
        return Collections.emptyList();
    }
}

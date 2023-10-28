package net.okocraft.velocitycommandaliases.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.okocraft.velocitycommandaliases.Main;
import ninja.leaping.configurate.ConfigurationNode;

public class CommandConfig extends CustomConfig {

    public CommandConfig(Main plugin) {
        super(plugin, "commands.yml");
    }

    public List<String> getChildren(String alias) {
        return getAliasesMap().getOrDefault(alias.toLowerCase(Locale.ROOT), Collections.emptyList());
    }

    public Map<String, List<String>> getAliasesMap() {
        Map<String, List<String>> result = new HashMap<>();
        ConfigurationNode aliasesSection = get().getNode("aliases");
        if (aliasesSection.isEmpty()) {
            return result;
        }

        aliasesSection.getChildrenMap().forEach((key, value) -> {
            String alias = key.toString();
            List<String> children = new ArrayList<>();

            if (value.getString() != null) {
                children.add(value.getString());
            } else if (value.isList()) {
                children.addAll(value.getList(Object::toString));
            }

            result.put(alias.toLowerCase(Locale.ROOT), Collections.unmodifiableList(children));
        });

        return result;
    }
}
package net.okocraft.velocitycommandaliases;

import com.google.inject.Inject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Path;
import net.okocraft.velocitycommandaliases.command.CommandAlias;
import net.okocraft.velocitycommandaliases.command.ReloadAliases;
import net.okocraft.velocitycommandaliases.config.CommandConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

public class Main {

    private boolean aliasesLoaded = false;

    private final CommandConfig commandConfig;

    private final ReloadAliases reloadCommand;

    private final Map<String, CommandAlias> commandAliases = new HashMap<>();
    private final Map<CommandMeta, LiteralCommandNode<CommandSource>> conflictingCommands = new HashMap<>();
    
    private final ProxyServer proxy;
    private final Path dataDirectory;
    private final Logger logger;
    
    
    @Inject
    public Main(ProxyServer proxy, @DataDirectory Path dataDirectory, Logger logger) {
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
        this.logger = logger;

        commandConfig = new CommandConfig(this);
        reloadCommand = new ReloadAliases(this);
    }

    public Path dataDirectory() {
        return this.dataDirectory;
    }

    public Logger logger() {
        return this.logger;
    }

    public ProxyServer proxy() {
        return this.proxy;
    }

    public boolean isAliasesLoaded() {
        return aliasesLoaded;
    }

    private void setAliasesLoaded(boolean aliasesLoaded) {
        this.aliasesLoaded = aliasesLoaded;
    }

    @Subscribe(order = PostOrder.LAST)
    public void onEnable(ProxyInitializeEvent event) {
        Map<String, CommandMeta> registeredCommands = new HashMap<>();

        for (String alias : proxy.getCommandManager().getAliases()) {
            registeredCommands.put(alias, proxy.getCommandManager().getCommandMeta(alias));
        }

        if (!registeredCommands.containsKey(reloadCommand.alias())) {
            CommandMeta reloadMeta = proxy.getCommandManager().metaBuilder(reloadCommand.alias())
                    .plugin(this).build();
            proxy.getCommandManager().register(reloadMeta, reloadCommand);
            reloadMeta.getAliases().forEach(a -> registeredCommands.put(a, reloadMeta));
        }

        commandConfig.saveDefault();
        Map<String, List<String>> aliasesMap = commandConfig.getAliasesMap();
        for (String aliasName : aliasesMap.keySet()) {
            CommandAlias alias = new CommandAlias(this, aliasName);
            commandAliases.put(aliasName, alias);

            CommandMeta conflicting = registeredCommands.get(aliasName);
            if (conflicting != null) {
                CommandNode<CommandSource> child = getDispatcher().getRoot().getChild(aliasName);
                if (child instanceof LiteralCommandNode<CommandSource>) {
                    conflictingCommands.put(conflicting, (LiteralCommandNode<CommandSource>) child);
                }
            }
        }

        Map<String, List<String>> invalidAliasesAndChildren = new HashMap<>();
        for (String aliasName : commandAliases.keySet()) {
            List<String> invalidChildren = new ArrayList<>(getLoopingRecursiveAlias(aliasName, aliasesMap));
            
            List<String> childNames = new ArrayList<>(aliasesMap.get(aliasName));
            childNames.replaceAll(child -> child.split(" ", -1)[0]);
            invalidChildren.retainAll(childNames);
            invalidChildren.remove(aliasName);

            for (String child : aliasesMap.get(aliasName)) {
                String childName = child.split(" ", -1)[0];
                if (!invalidChildren.contains(childName) && !registeredCommands.containsKey(childName)) {
                    invalidChildren.add(childName);
                }
            }

            if (!invalidChildren.isEmpty()) {
                invalidAliasesAndChildren.put(aliasName, invalidChildren);
            }
        }
        for (String invalidAlias : invalidAliasesAndChildren.keySet()) {
            String invalidChildren = String.join(", ", invalidAliasesAndChildren.get(invalidAlias).toArray(String[]::new));
            logger.warn("Could not register alias " + invalidAlias + " because it contains commands that do not exist: " + invalidChildren);
            commandAliases.remove(invalidAlias);
        }

        if (aliasesLoaded) {
            conflictingCommands.keySet().forEach(proxy.getCommandManager()::unregister);
            commandAliases.forEach((aliasName, alias) -> proxy.getCommandManager().register(
                    proxy.getCommandManager().metaBuilder(aliasName).plugin(this).build(),
                    alias
            ));
        } else {
            // prevent confliction with other plugin command with the same name.
            proxy.getScheduler().buildTask(this, () -> {
                conflictingCommands.keySet().forEach(proxy.getCommandManager()::unregister);
                commandAliases.forEach((aliasName, alias) -> proxy.getCommandManager().register(
                        proxy.getCommandManager().metaBuilder(aliasName).plugin(this).build(),
                        alias
                ));
                setAliasesLoaded(true);
            }).delay(10L, TimeUnit.SECONDS);
        }
    }

    private List<String> getLoopingRecursiveAlias(String aliasName, Map<String, List<String>> aliasesMap) {
        return getLoopingRecursiveAlias0(aliasName, new ArrayList<>(), -1, aliasesMap);
    }

    private List<String> getLoopingRecursiveAlias0(String aliasName, List<String> parents, int lastJunctionIndex, Map<String, List<String>> aliasesMap) {
        List<String> children = new ArrayList<>(aliasesMap.get(aliasName));
        children.replaceAll(child -> child.split(" ", -1)[0]);
        children.removeIf(child -> !aliasesMap.containsKey(child));

        if (!children.isEmpty()) {
            if (parents.contains(aliasName)) {
                return parents;
            }
            parents.add(aliasName);
            if (children.size() > 1) {
                lastJunctionIndex = parents.size() - 1;
            }
            aliasName = children.get(0);

        } else {
            if (lastJunctionIndex == -1) {
                return new ArrayList<>();
            }

            children = new ArrayList<>(aliasesMap.get(parents.get(lastJunctionIndex)));
            children.replaceAll(child -> child.split(" ", -1)[0]);
            children.removeIf(child -> !aliasesMap.containsKey(child));
            children.remove(parents.get(lastJunctionIndex + 1));

            aliasName = children.get(0);
            parents = new ArrayList<>(parents.subList(0, lastJunctionIndex + 1));
        }

        return getLoopingRecursiveAlias0(aliasName, parents, lastJunctionIndex, aliasesMap);
    }

    public void disableAlias(String aliasName) {
        proxy.getCommandManager().unregister(proxy.getCommandManager().getCommandMeta(aliasName));
        conflictingCommands.keySet().stream()
                .filter(m -> m.getAliases().contains(aliasName))
                .findFirst()
                .ifPresent(commandMeta -> proxy.getCommandManager().register(
                        commandMeta,
                        new BrigadierCommand(conflictingCommands.get(commandMeta))
                ));
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onDisable(ProxyShutdownEvent event) {
        commandAliases.keySet().forEach(this::disableAlias);
        commandAliases.clear();
        conflictingCommands.clear();
        
    }

    public CommandConfig getCommandConfig() {
        return commandConfig;
    }


    public CommandDispatcher<CommandSource> getDispatcher() {
        try {
            CommandManager commandManager = proxy.getCommandManager();
            return (CommandDispatcher<CommandSource>) commandManager.getClass().getField("dispatcher").get(commandManager);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

}
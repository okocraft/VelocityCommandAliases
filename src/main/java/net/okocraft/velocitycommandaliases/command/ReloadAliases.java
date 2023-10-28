package net.okocraft.velocitycommandaliases.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.Objects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.okocraft.velocitycommandaliases.Main;

public class ReloadAliases implements SimpleCommand {

    private final Main plugin;
    private final String alias;
    private final String permission;

    public ReloadAliases(Main plugin) {
        this.plugin = plugin;
        this.alias = "reloadaliases";
        this.permission = "velocitycommandaliases.reload";
    }

    public String alias() {
        return this.alias;
    }

    public String permission() {
        return this.permission;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        if (plugin.isAliasesLoaded()) {
            plugin.getCommandConfig().reload();
            plugin.onDisable(null);
            plugin.onEnable(null);
            sender.sendMessage(Component.text("Velocity command aliases successfully reloaded.")
                    .color(NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Velocity command aliases is not loaded yet.")
                    .color(NamedTextColor.RED));
        }
    }


    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ReloadAliases)) {
            return false;
        }
        ReloadAliases reloadAliases = (ReloadAliases) o;
        return Objects.equals(plugin, reloadAliases.plugin);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(plugin);
    }

    
}

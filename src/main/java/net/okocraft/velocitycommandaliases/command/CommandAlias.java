package net.okocraft.velocitycommandaliases.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.concurrent.ExecutionException;
import net.kyori.adventure.text.Component;
import net.okocraft.velocitycommandaliases.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandAlias implements SimpleCommand {

    private final Main plugin;
    private final String alias;

    public CommandAlias(Main plugin, String alias) {
        this.plugin = plugin;
        this.alias = alias;
    }

    private List<String> getReplacedChildren(String[] args, boolean forTabcompletion) throws IllegalArgumentException {
        List<String> children = new ArrayList<>(plugin.getCommandConfig().getChildren(alias));
        if (children.isEmpty()) {
            return new ArrayList<>();
        }
        
        for (int childIndex = 0; childIndex < children.size(); childIndex++) {
            String child = children.get(childIndex);
            for (int argIndex = 0; argIndex < args.length; argIndex++) {
                // extract `$x-` to `$x $x+1-` or `$$x-` to `$$x $x+1-`
                // if it is for tabcomplete, even on the last argument, `$x+1-` will be added.
                if (forTabcompletion || argIndex != args.length - 1) {
                    child = child.replaceAll("(((?<!\\\\)\\$)\\$?" + (argIndex + 1) + ")-", "$1 $2" + (argIndex + 2) + "-");
                } else {
                    child = child.replaceAll("(((?<!\\\\)\\$\\$?)" + (argIndex + 1) + ")-", "$1");
                }
                
                // finally `$x`s are replaced with input args, but `$x-` are not.
                child = child.replaceAll("(?<!\\\\)\\$\\$?" + (argIndex + 1) + "(?!-)", args[argIndex]);
            }

            // if it is not tabcomplete and there are `$$x`s throw exception.
            if (!forTabcompletion && child.matches(".*(?<!\\\\)\\$\\$(\\d+).*")) {
                throw new IllegalArgumentException("Missing required argument " + child.replaceAll(".*(?<!\\\\)\\$\\$(\\d+).*", "$1"));
            }
            children.set(childIndex, child);
        }

        return children;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();
        List<String> children;
        try {
            children = getReplacedChildren(args, false);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text(e.getMessage()));
            return;
        }

        for (String child : children) {
            plugin.proxy().getCommandManager().executeImmediatelyAsync(sender, child.replaceAll("(?<!\\\\)\\$(\\d+)-?", ""));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();
        List<String> completion = new ArrayList<>();

        for (String child : getReplacedChildren(Arrays.copyOfRange(args, 0, args.length - 1), true)) {
            if (child.matches(".*(?<!\\\\)\\$\\$?" + args.length + ".*")) {
                collectSuggestions(
                        sender,
                        child.replaceAll("(?<!\\\\)\\$\\$?" + args.length + ".*", "") + args[args.length - 1],
                        completion
                );
            }
        }

        return completion;
    }

    private void collectSuggestions(CommandSource source, String child, List<String> completion) {
        try {
            CommandDispatcher<CommandSource> dispatcher = plugin.getDispatcher();
            Suggestions suggestions;
            suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(child, source)).get();
            completion.addAll(suggestions.getList().stream().map(Suggestion::getText).toList());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}

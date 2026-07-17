package ru.rapidcoder.trader.bot.command;

import ru.rapidcoder.trader.bot.Bot;

import java.util.HashMap;
import java.util.Map;

public class CommandRegistry {

    private final Map<String, Command> commandMap = new HashMap<>();

    private final Command unknownCommand;

    public CommandRegistry(Bot bot) {
        this.unknownCommand = new UnknownCommand(bot);
    }

    public void registry(Command command) {
        commandMap.put(command.getIdentifier(), command);
    }

    public Command retrieveCommand(String commandIdentifier) {
        Command cmd = commandMap.get(commandIdentifier);
        if (cmd != null) {
            return cmd;
        }
        int spaceIndex = commandIdentifier.indexOf(' ');
        if (spaceIndex > 0) {
            cmd = commandMap.get(commandIdentifier.substring(0, spaceIndex));
            if (cmd != null) {
                return cmd;
            }
        }
        return unknownCommand;
    }

}

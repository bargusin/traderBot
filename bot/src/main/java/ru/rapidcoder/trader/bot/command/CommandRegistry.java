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
        return commandMap.getOrDefault(commandIdentifier, unknownCommand);
    }

}

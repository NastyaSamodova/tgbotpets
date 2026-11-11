package ru.samanas.petbot;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class BotMenuConfig {

    private final BotService botService;

    public BotMenuConfig(BotService botService) {
        this.botService = botService;
    }

    @PostConstruct
    public void setBotCommands() {
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("start", "Start the bot"));
        commands.add(new BotCommand("adopt", "Adopt a new pet"));
        commands.add(new BotCommand("status", "Check your pet's status"));
        commands.add(new BotCommand("feed", "Feed your pet"));
        commands.add(new BotCommand("name", "Give your pet a name (e.g. /name Buddy)"));
        commands.add(new BotCommand("release", "Release pet to a new family"));
        commands.add(new BotCommand("marry", "Marry someone (reply to their message)"));
        commands.add(new BotCommand("play", "Play with your pet"));
        commands.add(new BotCommand("school", "Send to school/work"));

        SetMyCommands setMyCommands = new SetMyCommands(commands, new BotCommandScopeDefault(), null);
        try {
            botService.execute(setMyCommands);
            System.out.println("✅ Меню команд успешно установлено!");
        } catch (TelegramApiException e) {
            System.err.println("❌ Ошибка установки меню: " + e.getMessage());
        }
    }
}
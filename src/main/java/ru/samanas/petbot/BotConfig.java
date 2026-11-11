package ru.samanas.petbot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(BotService botService) {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(botService);
            System.out.println("=== БОТ УСПЕШНО ЗАРЕГИСТРИРОВАН В TELEGRAM API ===");
            return api;
        } catch (TelegramApiException e) {
            System.err.println("=== ОШИБКА РЕГИСТРАЦИИ: " + e.getMessage() + " ===");
            return null;
        }
    }
}
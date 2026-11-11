package ru.samanas.petbot;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.samanas.petbot.entity.Pet;
import ru.samanas.petbot.entity.Union;
import ru.samanas.petbot.entity.User;
import ru.samanas.petbot.repository.PetRepository;
import ru.samanas.petbot.repository.UnionRepository;
import ru.samanas.petbot.repository.UserRepository;
import ru.samanas.petbot.service.GameService;

import java.util.ArrayList;
import java.util.List;

@Component
public class BotService extends TelegramLongPollingBot {

    private final GameService gameService;
    private final PetRepository petRepository;
    private final UnionRepository unionRepository;
    private final UserRepository userRepository;

    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String botToken;

    public BotService(GameService gameService, PetRepository petRepository, UnionRepository unionRepository, UserRepository userRepository) {
        this.gameService = gameService;
        this.petRepository = petRepository;
        this.unionRepository = unionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String getBotUsername() { return botName; }
    @Override
    public String getBotToken() { return botToken; }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            Long userId = update.getMessage().getFrom().getId();
            String command = messageText.split("@")[0];

            if (!command.startsWith("/")) return;

            if (command.equalsIgnoreCase("/marry")) {
                if (update.getMessage().isReply()) {
                    Long proposerId = update.getMessage().getFrom().getId();
                    String proposerName = update.getMessage().getFrom().getUserName();
                    Long targetId = update.getMessage().getReplyToMessage().getFrom().getId();
                    String targetName = update.getMessage().getReplyToMessage().getFrom().getUserName();
                    if (proposerName == null) proposerName = "Someone";
                    if (targetName == null) targetName = "Someone";
                    sendText(chatId, gameService.createUnion(proposerId, proposerName, targetId, targetName, chatId));
                } else {
                    sendText(chatId, "To propose, reply to someone's message with /marry ❤️");
                }
            }
            else if (command.equalsIgnoreCase("/adopt")) {
                String response = gameService.adoptPet(userId);
                if (!response.startsWith("You already") && !response.startsWith("❌")) {
                    sendPetPhoto(chatId, "egg.jpg", response);
                } else {
                    sendText(chatId, response);
                }
            }
            else if (command.equalsIgnoreCase("/status")) {
                String response = gameService.getPetStatus(userId);
                var unionOpt = unionRepository.findByUser1IdOrUser2Id(userId, userId);
                if (unionOpt.isPresent()) {
                    var petOpt = petRepository.findByUnionId(unionOpt.get().getId());
                    if (petOpt.isPresent()) {
                        Pet pet = petOpt.get();
                        String imageName = pet.getSpecies().name().toLowerCase() + ".jpg";
                        sendPetPhoto(chatId, imageName, response);
                        return;
                    }
                }
                sendText(chatId, response);
            }
            else if (command.equalsIgnoreCase("/feed")) {
                sendText(chatId, gameService.feedPet(userId));
            }
            else if (command.equalsIgnoreCase("/play")) {
                sendText(chatId, gameService.playPet(userId));
            }
            else if (command.equalsIgnoreCase("/school")) {
                sendText(chatId, gameService.studyPet(userId));
            }
            else if (command.equalsIgnoreCase("/release")) {
                sendText(chatId, gameService.releasePet(userId));
            }
            else if (command.startsWith("/name")) {
                String[] parts = messageText.split(" ", 2);
                if (parts.length < 2) {
                    sendText(chatId, "Please provide a name. Example: /name Buddy");
                } else {
                    sendText(chatId, gameService.renamePet(userId, parts[1]));
                }
            }
        }
    }

    // --- ТАЙМЕР И УВЕДОМЛЕНИЯ ---
    @Scheduled(fixedRate = 60000) // Каждые 60 секунд
    public void scheduledPetUpdate() {
        List<Pet> allPets = petRepository.findAll();

        // Отладка: мы увидим в консоли, что таймер работает
        if (!allPets.isEmpty()) {
            System.out.println("Таймер тикнул! Проверяем " + allPets.size() + " питомцев...");
        }

        for (Pet pet : allPets) {
            pet.setHunger(Math.max(0, pet.getHunger() - 2));
            pet.setHappiness(Math.max(0, pet.getHappiness() - 1));

            if (pet.getHunger() < 30 && !pet.isNotifiedHungry()) {
                Union union = unionRepository.findById(pet.getUnionId()).orElse(null);

                if (union != null && union.getChatId() != null) {
                    User user1 = userRepository.findById(union.getUser1Id()).orElse(null);
                    User user2 = userRepository.findById(union.getUser2Id()).orElse(null);

                    // Используем HTML-разметку для тега (работает без @username)
                    String tag1 = getTag(user1, "Mom");
                    String tag2 = getTag(user2, "Dad");

                    String alertMessage = "🚨 " + tag1 + " " + tag2 + ", your pet <b>" + pet.getName() + "</b> is hungry! (Hunger: " + pet.getHunger() + "/100)\nType /feed to feed them!";

                    // Отправляем с поддержкой HTML
                    SendMessage message = new SendMessage(union.getChatId().toString(), alertMessage);
                    message.setParseMode("HTML"); // Включаем HTML
                    try {
                        execute(message);
                        System.out.println("Уведомление успешно отправлено в чат " + union.getChatId());
                    } catch (TelegramApiException e) {
                        System.err.println("Ошибка отправки уведомления: " + e.getMessage());
                    }

                    pet.setNotifiedHungry(true);
                } else {
                    System.out.println("Ошибка: У союза " + pet.getUnionId() + " нет chatId (возможно, союз был создан до обновления). Пересоздайте брак!");
                }
            }
            petRepository.save(pet);
        }
    }

    // Вспомогательный метод для создания тега пользователя (по ID)
    private String getTag(User user, String fallbackName) {
        if (user == null) return fallbackName;
        // Извлекаем имя (или используем username, если есть)
        String displayName = user.getUsername() != null ? user.getUsername() : "Player";
        // Делаем ссылку на профиль по ID (работает всегда)
        return "<a href='tg://user?id=" + user.getTelegramId() + "'>" + displayName + "</a>";
    }

    // --- ОСТАЛЬНЫЕ МЕТОДЫ ---
    private void sendText(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("HTML"); // Поддержка HTML
        message.setReplyMarkup(getMainMenu());
        try { execute(message); } catch (TelegramApiException e) { System.err.println(e.getMessage()); }
    }

    private void sendPetPhoto(Long chatId, String imageName, String caption) {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(chatId.toString());
        photo.setParseMode("HTML"); // Поддержка HTML
        photo.setReplyMarkup(getMainMenu());
        try {
            org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource("images/" + imageName);
            if (!resource.exists()) { sendText(chatId, caption); return; }
            photo.setPhoto(new InputFile(resource.getInputStream(), imageName));
            photo.setCaption(caption);
            execute(photo);
        } catch (Exception e) { sendText(chatId, caption); }
    }

    private ReplyKeyboardMarkup getMainMenu() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow(); row1.add("/status"); row1.add("/feed");
        KeyboardRow row2 = new KeyboardRow(); row2.add("/play"); row2.add("/school");
        KeyboardRow row3 = new KeyboardRow(); row3.add("/adopt"); row3.add("/release");
        keyboard.add(row1); keyboard.add(row2); keyboard.add(row3);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    @PostConstruct
    public void init() { System.out.println("Bot is running!"); }
}
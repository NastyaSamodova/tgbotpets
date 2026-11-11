package ru.samanas.petbot.service;

import org.springframework.stereotype.Service;
import ru.samanas.petbot.entity.Pet;
import ru.samanas.petbot.entity.Union;
import ru.samanas.petbot.entity.User;
import ru.samanas.petbot.enums.AnimalType;
import ru.samanas.petbot.enums.FoodType;
import ru.samanas.petbot.enums.PetStage;
import ru.samanas.petbot.repository.PetRepository;
import ru.samanas.petbot.repository.UnionRepository;
import ru.samanas.petbot.repository.UserRepository;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class GameService {

    private final PetRepository petRepository;
    private final UnionRepository unionRepository;
    private final UserRepository userRepository;

    public GameService(PetRepository petRepository, UnionRepository unionRepository, UserRepository userRepository) {
        this.petRepository = petRepository;
        this.unionRepository = unionRepository;
        this.userRepository = userRepository;
    }

    public String adoptPet(Long userId) {
        var unionOpt = unionRepository.findByUser1IdOrUser2Id(userId, userId);
        if (unionOpt.isEmpty()) return "❌ You must be in a union to adopt a pet! Use /marry first.";
        Union union = unionOpt.get();

        if (petRepository.findByUnionId(union.getId()).isPresent()) {
            return "You already have a pet in your union! You can't adopt another one.";
        }

        Pet pet = new Pet();
        pet.setUnionId(union.getId());
        pet.setName("Baby");
        pet.setSpecies(AnimalType.values()[ThreadLocalRandom.current().nextInt(AnimalType.values().length)]);
        pet.setFavoriteFood(FoodType.values()[ThreadLocalRandom.current().nextInt(FoodType.values().length)]);
        pet.setStage(PetStage.EGG);
        pet.setHunger(100);
        pet.setHappiness(100);
        pet.setExperience(0);

        petRepository.save(pet);

        return "🎉 Congratulations! A new pet hatched from the egg!\n\n" +
                "Species: " + pet.getSpecies() + "\n" +
                "Stage: " + pet.getStage() + "\n" +
                "Hunger: " + pet.getHunger() + "/100\n" +
                "Happiness: " + pet.getHappiness() + "/100\n" +
                "Favorite Food: " + pet.getFavoriteFood() + "\n\n" +
                "👉 Let's give your new friend a name!\n" +
                "Type: /name [pet_name] (for example: /name Buddy)";
    }

    public String getPetStatus(Long userId) {
        var unionOpt = unionRepository.findByUser1IdOrUser2Id(userId, userId);
        if (unionOpt.isEmpty()) return "You are not in a union. Use /marry!";

        return petRepository.findByUnionId(unionOpt.get().getId())
                .map(pet -> {
                    int xp = pet.getExperience();
                    String progressText = "";

                    if (pet.getStage() == PetStage.EGG) {
                        progressText = "🥚 XP: " + xp + "/100 [" + (xp * 100 / 100) + "%] to Kindergarten";
                    } else if (pet.getStage() == PetStage.KINDERGARTEN) {
                        progressText = "🏫 XP: " + xp + "/300 [" + (xp * 100 / 300) + "%] to School";
                    } else if (pet.getStage() == PetStage.SCHOOL) {
                        progressText = "🎓 XP: " + xp + "/600 [" + (xp * 100 / 600) + "%] to Adult";
                    } else {
                        progressText = "👑 XP: " + xp + " (Max Level)";
                    }

                    return "🐾 Name: " + pet.getName() + "\n" +
                            "🦊 Species: " + pet.getSpecies() + "\n" +
                            "🎓 Stage: " + pet.getStage() + "\n" +
                            "✨ " + progressText + "\n" +
                            "🍖 Hunger: " + pet.getHunger() + "/100\n" +
                            "😊 Happiness: " + pet.getHappiness() + "/100\n" +
                            "🥕 Favorite food: " + pet.getFavoriteFood();
                })
                .orElse("Your union doesn't have a pet yet! Type /adopt.");
    }

    public String feedPet(Long userId) {
        return performAction(userId, "fed", 20, 0, 10);
    }

    public String playPet(Long userId) {
        return performAction(userId, "played with", -10, 20, 15);
    }

    // НОВАЯ ЛОГИКА ДЛЯ ШКОЛЫ/РАБОТЫ
    public String studyPet(Long userId) {
        var unionOpt = unionRepository.findByUser1IdOrUser2Id(userId, userId);
        if (unionOpt.isEmpty()) return "You are not in a union!";
        var petOpt = petRepository.findByUnionId(unionOpt.get().getId());
        if (petOpt.isEmpty()) return "Your union doesn't have a pet yet! Type /adopt.";

        Pet pet = petOpt.get();

        if (pet.getStage() == PetStage.EGG) {
            return "🥚 Your pet is still an egg. It can't go anywhere yet!";
        }

        String action = "";
        if (pet.getStage() == PetStage.KINDERGARTEN) action = "went to kindergarten";
        else if (pet.getStage() == PetStage.SCHOOL) action = "went to school";
        else if (pet.getStage() == PetStage.ADULT) action = "went to work";

        // Учеба/работа отнимает много сытости и счастья, но дает много опыта
        return performAction(userId, action, -15, -10, 30);
    }

    private String performAction(Long userId, String actionName, int hungerChange, int happinessChange, int xpChange) {
        var unionOpt = unionRepository.findByUser1IdOrUser2Id(userId, userId);
        if (unionOpt.isEmpty()) return "You are not in a union!";

        var petOpt = petRepository.findByUnionId(unionOpt.get().getId());
        if (petOpt.isEmpty()) return "Your union doesn't have a pet yet! Type /adopt.";

        Pet pet = petOpt.get();

        pet.setHunger(Math.max(0, Math.min(100, pet.getHunger() + hungerChange)));
        pet.setHappiness(Math.max(0, Math.min(100, pet.getHappiness() + happinessChange)));
        pet.setExperience(pet.getExperience() + xpChange);

        // Если покормили, сбрасываем флаг голода
        if (hungerChange > 0) {
            pet.setNotifiedHungry(false);
        }

        String evolutionMessage = checkEvolution(pet);
        petRepository.save(pet);

        return "Success! You " + actionName + " " + pet.getName() + ".\n" +
                "🍖 Hunger: " + pet.getHunger() + "/100\n" +
                "😊 Happiness: " + pet.getHappiness() + "/100\n" +
                "✨ XP: +" + xpChange + evolutionMessage;
    }

    private String checkEvolution(Pet pet) {
        int xp = pet.getExperience();
        PetStage currentStage = pet.getStage();

        if (currentStage == PetStage.EGG && xp >= 100) {
            pet.setStage(PetStage.KINDERGARTEN);
            return "\n\n🎉 WOW! Your pet evolved into a **KINDERGARTENER**!";
        } else if (currentStage == PetStage.KINDERGARTEN && xp >= 300) {
            pet.setStage(PetStage.SCHOOL);
            return "\n\n🎉 WOW! Your pet evolved into a **SCHOOL STUDENT**!";
        } else if (currentStage == PetStage.SCHOOL && xp >= 600) {
            pet.setStage(PetStage.ADULT);
            return "\n\n👑 CONGRATULATIONS! Your pet is now an **ADULT**!";
        }
        return "";
    }

    public String releasePet(Long userId) {
        var unionOpt = unionRepository.findByUser1IdOrUser2Id(userId, userId);
        if (unionOpt.isEmpty()) return "You are not in a union!";

        var petOpt = petRepository.findByUnionId(unionOpt.get().getId());
        if (petOpt.isEmpty()) return "Your union doesn't have a pet to release.";

        Pet pet = petOpt.get();
        petRepository.delete(pet);
        return "You released " + pet.getName() + " to a new family. 🏡";
    }

    public String renamePet(Long userId, String newName) {
        var unionOpt = unionRepository.findByUser1IdOrUser2Id(userId, userId);
        if (unionOpt.isEmpty()) return "You are not in a union!";

        var petOpt = petRepository.findByUnionId(unionOpt.get().getId());
        if (petOpt.isEmpty()) return "Your union doesn't have a pet to rename!";

        Pet pet = petOpt.get();
        if (newName.length() > 20) return "Name is too long! Max 20 characters.";

        pet.setName(newName);
        petRepository.save(pet);
        return "✨ Success! Your pet's name is now: " + newName;
    }

    // ОБНОВЛЕНО: теперь передаем chatId, чтобы бот знал, куда писать уведомления
    public String createUnion(Long proposerId, String proposerName, Long targetId, String targetName, Long chatId) {
        if (proposerId.equals(targetId)) return "You can't marry yourself! 🙃";
        if (unionRepository.existsByUser1IdOrUser2Id(proposerId, proposerId)) return "You are already in a union!";
        if (unionRepository.existsByUser1IdOrUser2Id(targetId, targetId)) return targetName + " is already in a union!";

        createUserIfNotExists(proposerId, proposerName);
        createUserIfNotExists(targetId, targetName);

        Union union = new Union();
        union.setUser1Id(proposerId);
        union.setUser2Id(targetId);
        union.setChatId(chatId); // Сохраняем ID группы
        unionRepository.save(union);

        return "🎉 Congratulations! " + proposerName + " and " + targetName + " are now in a union! ❤️\n" +
                "Type /adopt to share a pet together!";
    }

    private void createUserIfNotExists(Long telegramId, String username) {
        if (!userRepository.existsById(telegramId)) {
            User user = new User();
            user.setTelegramId(telegramId);
            user.setUsername(username);
            userRepository.save(user);
        }
    }
}
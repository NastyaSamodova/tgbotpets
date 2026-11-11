package ru.samanas.petbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.samanas.petbot.enums.AnimalType;
import ru.samanas.petbot.enums.FoodType;
import ru.samanas.petbot.enums.PetStage;

@Entity
@Table(name = "pets")
@Data
@NoArgsConstructor
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private AnimalType species;

    @Enumerated(EnumType.STRING)
    private PetStage stage;

    private int hunger = 100;
    private int happiness = 100;

    @Enumerated(EnumType.STRING)
    private FoodType favoriteFood;

    // БЫЛО: @Column(name = "chat_id") private Long chatId;
    // СТАЛО:
    @Column(name = "union_id")
    private Long unionId;

    // Добавьте эту строчку к остальным полям
    private int experience = 0;

    // По умолчанию false. Когда бот предупредит, что питомец голоден, станет true
    private boolean notifiedHungry = false;
}
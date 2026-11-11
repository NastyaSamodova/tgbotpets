package ru.samanas.petbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    private Long telegramId; // ID пользователя в ТГ (он уникален, поэтому @Id)

    private String username; // Имя для красоты
}
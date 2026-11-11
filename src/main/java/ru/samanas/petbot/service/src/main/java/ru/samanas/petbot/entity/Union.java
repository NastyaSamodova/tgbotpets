package ru.samanas.petbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "unions")
@Data
@NoArgsConstructor
public class Union {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user1_id")
    private Long user1Id; // Первый партнер

    @Column(name = "user2_id")
    private Long user2Id; // Второй партнер

    @Column(name = "chat_id")
    private Long chatId;

}
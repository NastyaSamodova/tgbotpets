package ru.samanas.petbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.samanas.petbot.entity.Union;
import java.util.Optional;

@Repository
public interface UnionRepository extends JpaRepository<Union, Long> {
    boolean existsByUser1IdOrUser2Id(Long user1Id, Long user2Id);
    // НОВЫЙ МЕТОД: найти союз, где пользователь является первым ИЛИ вторым партнером
    Optional<Union> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);
}
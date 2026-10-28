package br.finedu.calculator;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationRepository extends JpaRepository<Simulation, Long> {

    List<Simulation> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Simulation> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);
}

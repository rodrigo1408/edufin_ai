package br.finedu;

import static org.assertj.core.api.Assertions.assertThat;

import br.finedu.course.CourseRepository;
import br.finedu.quiz.QuizRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Valida as migrações e o mapeamento JPA no PostgreSQL real. Executa somente quando há Docker
 * disponível (caso contrário é marcado como ignorado, sem falso positivo).
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    CourseRepository courses;

    @Autowired
    QuizRepository quizzes;

    @Test
    void migrationsApplyAndQueriesWorkOnPostgres() {
        assertThat(courses.findPublished(null, null)).hasSize(6);
        assertThat(quizzes.findPublished(null, null)).hasSize(7);
    }
}

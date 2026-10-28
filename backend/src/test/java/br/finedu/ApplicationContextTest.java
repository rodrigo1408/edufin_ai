package br.finedu;

import static org.assertj.core.api.Assertions.assertThat;

import br.finedu.course.CourseRepository;
import br.finedu.gamification.AchievementRepository;
import br.finedu.quiz.QuizRepository;
import br.finedu.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Sobe a aplicação: aplica as migrações, valida o mapeamento JPA e confere o conteúdo inicial. */
class ApplicationContextTest extends IntegrationTest {

    @Autowired
    CourseRepository courses;

    @Autowired
    QuizRepository quizzes;

    @Autowired
    AchievementRepository achievements;

    @Test
    @Transactional
    void migrationsSeedEducationalContent() {
        assertThat(courses.findPublished(null, null)).hasSize(6);
        assertThat(courses.findPublished(null, null))
                .allSatisfy(c -> assertThat(c.orderedLessons()).isNotEmpty());
        assertThat(quizzes.findPublished(null, null)).hasSize(7)
                .allSatisfy(q -> {
                    assertThat(q.getQuestions()).hasSizeGreaterThanOrEqualTo(4);
                    q.getQuestions().forEach(question ->
                            assertThat(question.getOptions().stream().filter(o -> o.isCorrect()).count()).isEqualTo(1));
                });
        assertThat(achievements.count()).isEqualTo(14);
    }
}

package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Conteúdo educativo inicial (conquistas, quizzes e cursos). Migração versionada em Java para
 * obter as chaves geradas sem depender de IDs fixos, funcionando igual em PostgreSQL e H2.
 * Os textos ficam em {@link SeedCourses} e {@link SeedQuizzes}.
 */
public class V2__SeedContent extends BaseJavaMigration {

    record Option(String text, boolean correct) {
    }

    record Question(String topic, String statement, String explanation, List<Option> options) {
    }

    record QuizSeed(String key, String title, String description, String category, String difficulty,
                    List<Question> questions) {
    }

    record LessonSeed(String title, int minutes, String quizKey, String content) {
    }

    record ModuleSeed(String title, List<LessonSeed> lessons) {
    }

    record CourseSeed(String slug, String title, String summary, String description, String objectives,
                      String category, String difficulty, int minutes, List<ModuleSeed> modules) {
    }

    static Option ok(String text) {
        return new Option(text, true);
    }

    static Option no(String text) {
        return new Option(text, false);
    }

    @Override
    public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        Timestamp now = Timestamp.from(Instant.parse("2026-01-01T00:00:00Z"));
        seedAchievements(c);
        java.util.Map<String, Long> quizIds = new java.util.HashMap<>();
        for (QuizSeed q : SeedQuizzes.ALL) {
            quizIds.put(q.key(), insertQuiz(c, q, now));
        }
        int position = 1;
        for (CourseSeed course : SeedCourses.ALL) {
            insertCourse(c, course, position++, quizIds, now);
        }
    }

    private void seedAchievements(Connection c) throws Exception {
        Object[][] rows = {
                {"FIRST_LESSON", "Primeira aula", "Concluiu sua primeira aula.", "book", 10},
                {"FIRST_COURSE", "Curso concluído", "Concluiu um curso completo.", "graduation", 30},
                {"THREE_COURSES", "Trilha em andamento", "Concluiu 3 cursos.", "trophy", 60},
                {"FIRST_QUIZ", "Primeiro quiz", "Finalizou seu primeiro quiz.", "help", 10},
                {"QUIZ_PASS_3", "Aprovado(a) x3", "Foi aprovado(a) em 3 quizzes diferentes.", "check", 30},
                {"QUIZ_PERFECT", "Gabaritou!", "Acertou 100% de um quiz.", "star", 20},
                {"FIRST_GOAL", "Sonhar com plano", "Criou sua primeira meta financeira.", "target", 10},
                {"GOAL_ACHIEVED", "Meta alcançada", "Concluiu uma meta financeira.", "flag", 30},
                {"BUDGET_TRACKER", "Diário em dia", "Registrou movimentações em 7 dias diferentes.", "calendar", 30},
                {"FIRST_SIMULATION", "Simulador(a)", "Salvou sua primeira simulação de juros.", "calculator", 10},
                {"FIRST_WALLET", "Investidor(a) de laboratório", "Criou uma carteira simulada.", "flask", 10},
                {"STREAK_3", "Ritmo bom", "Aprendeu 3 dias seguidos.", "flame", 15},
                {"STREAK_7", "Semana completa", "Aprendeu 7 dias seguidos.", "zap", 40},
                {"CHALLENGE_1", "Desafiante", "Concluiu seu primeiro desafio semanal.", "medal", 20},
        };
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO achievements (code, title, description, icon, points) VALUES (?, ?, ?, ?, ?)")) {
            for (Object[] r : rows) {
                ps.setString(1, (String) r[0]);
                ps.setString(2, (String) r[1]);
                ps.setString(3, (String) r[2]);
                ps.setString(4, (String) r[3]);
                ps.setInt(5, (Integer) r[4]);
                ps.executeUpdate();
            }
        }
    }

    private long insertQuiz(Connection c, QuizSeed q, Timestamp now) throws Exception {
        long quizId = insert(c, "INSERT INTO quizzes (title, description, category, difficulty, published, created_at, "
                + "updated_at) VALUES (?, ?, ?, ?, TRUE, ?, ?)", q.title(), q.description(), q.category(),
                q.difficulty(), now, now);
        int qp = 1;
        for (Question question : q.questions()) {
            long questionId = insert(c, "INSERT INTO quiz_questions (quiz_id, position, statement, explanation, topic) "
                    + "VALUES (?, ?, ?, ?, ?)", quizId, qp++, question.statement(), question.explanation(), question.topic());
            int op = 1;
            for (Option o : question.options()) {
                insert(c, "INSERT INTO quiz_options (question_id, position, text, correct) VALUES (?, ?, ?, ?)",
                        questionId, op++, o.text(), o.correct());
            }
        }
        return quizId;
    }

    private void insertCourse(Connection c, CourseSeed s, int position, java.util.Map<String, Long> quizIds,
                              Timestamp now) throws Exception {
        long courseId = insert(c, "INSERT INTO courses (slug, title, summary, description, objectives, category, "
                        + "difficulty, estimated_minutes, position, published, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE, ?, ?)", s.slug(), s.title(), s.summary(),
                s.description(), s.objectives(), s.category(), s.difficulty(), s.minutes(), position, now, now);
        int mp = 1;
        for (ModuleSeed m : s.modules()) {
            long moduleId = insert(c, "INSERT INTO course_modules (course_id, position, title) VALUES (?, ?, ?)",
                    courseId, mp++, m.title());
            int lp = 1;
            for (LessonSeed l : m.lessons()) {
                Long quizId = l.quizKey() == null ? null : quizIds.get(l.quizKey());
                insert(c, "INSERT INTO lessons (module_id, position, title, content, estimated_minutes, quiz_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)", moduleId, lp++, l.title(), l.content().strip(), l.minutes(), quizId);
            }
        }
    }

    private static long insert(Connection c, String sql, Object... params) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(sql, new String[]{"id"})) {
            for (int i = 0; i < params.length; i++) {
                Object p = params[i];
                if (p == null) {
                    ps.setNull(i + 1, java.sql.Types.BIGINT);
                } else {
                    ps.setObject(i + 1, p);
                }
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new IllegalStateException("Chave não gerada: " + sql);
    }
}

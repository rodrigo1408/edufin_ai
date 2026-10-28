package db.migration;

import db.migration.V2__SeedContent.CourseSeed;
import db.migration.V2__SeedContent.LessonSeed;
import db.migration.V2__SeedContent.ModuleSeed;
import db.migration.V2__SeedContent.Option;
import db.migration.V2__SeedContent.Question;
import db.migration.V2__SeedContent.QuizSeed;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * A plataforma passou a ser para todas as idades (não só adolescentes). Bancos criados antes desta
 * versão recebem os textos revisados de {@link SeedCourses} e {@link SeedQuizzes}. A localização é
 * por slug/título e posição, então conteúdo criado pela administração não é alterado.
 */
public class V3__AudienceNeutralContent extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection c = context.getConnection();
        try (PreparedStatement ps = c.prepareStatement("UPDATE lessons SET content = ? WHERE position = ? AND module_id IN "
                + "(SELECT m.id FROM course_modules m JOIN courses co ON co.id = m.course_id "
                + "WHERE co.slug = ? AND m.position = ?)")) {
            for (CourseSeed course : SeedCourses.ALL) {
                int mp = 1;
                for (ModuleSeed m : course.modules()) {
                    int lp = 1;
                    for (LessonSeed l : m.lessons()) {
                        ps.setString(1, l.content().strip());
                        ps.setInt(2, lp++);
                        ps.setString(3, course.slug());
                        ps.setInt(4, mp);
                        ps.executeUpdate();
                    }
                    mp++;
                }
            }
        }
        try (PreparedStatement question = c.prepareStatement("UPDATE quiz_questions SET statement = ?, explanation = ? "
                + "WHERE position = ? AND quiz_id IN (SELECT id FROM quizzes WHERE title = ?)");
             PreparedStatement option = c.prepareStatement("UPDATE quiz_options SET text = ? WHERE position = ? AND "
                     + "question_id IN (SELECT qq.id FROM quiz_questions qq JOIN quizzes q ON q.id = qq.quiz_id "
                     + "WHERE q.title = ? AND qq.position = ?)")) {
            for (QuizSeed quiz : SeedQuizzes.ALL) {
                int qp = 1;
                for (Question q : quiz.questions()) {
                    question.setString(1, q.statement());
                    question.setString(2, q.explanation());
                    question.setInt(3, qp);
                    question.setString(4, quiz.title());
                    question.executeUpdate();
                    int op = 1;
                    for (Option o : q.options()) {
                        option.setString(1, o.text());
                        option.setInt(2, op++);
                        option.setString(3, quiz.title());
                        option.setInt(4, qp);
                        option.executeUpdate();
                    }
                    qp++;
                }
            }
        }
    }
}

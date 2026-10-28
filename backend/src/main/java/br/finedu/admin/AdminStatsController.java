package br.finedu.admin;

import br.finedu.course.CourseRepository;
import br.finedu.feedback.Feedback;
import br.finedu.feedback.FeedbackRepository;
import br.finedu.forum.ForumEntities.ReportStatus;
import br.finedu.forum.ForumReportRepository;
import br.finedu.quiz.QuizRepository;
import br.finedu.security.AuthUser;
import br.finedu.security.Permission;
import br.finedu.user.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Visão geral da área administrativa: somente contagens agregadas, nunca dados financeiros individuais. */
@RestController
public class AdminStatsController {

    private final UserRepository users;
    private final CourseRepository courses;
    private final QuizRepository quizzes;
    private final ForumReportRepository reports;
    private final FeedbackRepository feedback;

    public AdminStatsController(UserRepository users, CourseRepository courses, QuizRepository quizzes,
                                ForumReportRepository reports, FeedbackRepository feedback) {
        this.users = users;
        this.courses = courses;
        this.quizzes = quizzes;
        this.reports = reports;
        this.feedback = feedback;
    }

    @GetMapping("/api/admin/stats")
    @Transactional(readOnly = true)
    public Map<String, Object> stats(@AuthenticationPrincipal AuthUser user) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (user.has(Permission.USER_MANAGE)) {
            map.put("users", users.count());
        }
        if (user.has(Permission.CONTENT_MANAGE)) {
            map.put("courses", courses.count());
            map.put("quizzes", quizzes.count());
        }
        if (user.has(Permission.FORUM_MODERATE)) {
            map.put("openReports", reports.countByStatus(ReportStatus.OPEN));
        }
        if (user.has(Permission.FEEDBACK_MANAGE)) {
            map.put("newFeedback", feedback.countByStatus(Feedback.Status.NEW));
        }
        map.put("permissions", user.role().permissions());
        return map;
    }
}

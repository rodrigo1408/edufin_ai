package br.finedu.admin;

import br.finedu.admin.AdminContentDtos.CourseAdminView;
import br.finedu.admin.AdminContentDtos.CourseRequest;
import br.finedu.admin.AdminContentDtos.LessonAdminView;
import br.finedu.admin.AdminContentDtos.LessonRequest;
import br.finedu.admin.AdminContentDtos.ModuleAdminView;
import br.finedu.admin.AdminContentDtos.OptionAdminView;
import br.finedu.admin.AdminContentDtos.QuestionAdminView;
import br.finedu.admin.AdminContentDtos.QuestionRequest;
import br.finedu.admin.AdminContentDtos.QuizAdminView;
import br.finedu.admin.AdminContentDtos.QuizCreateRequest;
import br.finedu.admin.AdminContentDtos.QuizMetaRequest;
import br.finedu.common.ApiException;
import br.finedu.common.AppTime;
import br.finedu.course.Course;
import br.finedu.course.CourseModule;
import br.finedu.course.CourseModuleRepository;
import br.finedu.course.CourseRepository;
import br.finedu.course.EnrollmentRepository;
import br.finedu.course.Lesson;
import br.finedu.course.LessonRepository;
import br.finedu.quiz.Quiz;
import br.finedu.quiz.QuizAttemptRepository;
import br.finedu.quiz.QuizOption;
import br.finedu.quiz.QuizQuestion;
import br.finedu.quiz.QuizRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestão de cursos e quizzes por educadores e administradores (permissão CONTENT_MANAGE). */
@Service
public class AdminContentService {

    private final CourseRepository courses;
    private final CourseModuleRepository modules;
    private final LessonRepository lessons;
    private final EnrollmentRepository enrollments;
    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final AppTime time;

    public AdminContentService(CourseRepository courses, CourseModuleRepository modules, LessonRepository lessons,
                               EnrollmentRepository enrollments, QuizRepository quizzes, QuizAttemptRepository attempts,
                               AppTime time) {
        this.courses = courses;
        this.modules = modules;
        this.lessons = lessons;
        this.enrollments = enrollments;
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.time = time;
    }

    // ------------------------------------------------------------------ cursos

    @Transactional(readOnly = true)
    public List<CourseAdminView> listCourses() {
        return courses.findAllByOrderByPositionAscIdAsc().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public CourseAdminView getCourse(Long id) {
        return view(course(id));
    }

    @Transactional
    public CourseAdminView createCourse(CourseRequest req) {
        if (courses.existsBySlug(req.slug())) {
            throw ApiException.conflict("SLUG_IN_USE", "Já existe um curso com este identificador.");
        }
        Course c = new Course(req.slug(), time.now());
        apply(c, req);
        return view(courses.save(c));
    }

    @Transactional
    public CourseAdminView updateCourse(Long id, CourseRequest req) {
        Course c = course(id);
        if (!c.getSlug().equals(req.slug())) {
            throw ApiException.business("SLUG_IMMUTABLE", "O identificador (slug) não pode ser alterado.");
        }
        apply(c, req);
        return view(c);
    }

    /** Cursos com inscrições não são excluídos (preserva o histórico): devem ser despublicados. */
    @Transactional
    public void deleteCourse(Long id) {
        Course c = course(id);
        if (enrollments.countByCourseId(id) > 0) {
            throw ApiException.conflict("COURSE_IN_USE", "O curso possui inscrições. Despublique-o em vez de excluir.");
        }
        courses.delete(c);
    }

    @Transactional
    public CourseAdminView addModule(Long courseId, String title) {
        Course c = course(courseId);
        int next = c.getModules().stream().mapToInt(CourseModule::getPosition).max().orElse(0) + 1;
        c.getModules().add(new CourseModule(c, next, title.trim()));
        courses.flush();
        return view(c);
    }

    @Transactional
    public CourseAdminView renameModule(Long moduleId, String title) {
        CourseModule m = modules.findWithCourse(moduleId).orElseThrow(() -> ApiException.notFound("Módulo"));
        m.rename(title.trim());
        return view(m.getCourse());
    }

    @Transactional
    public CourseAdminView deleteModule(Long moduleId) {
        CourseModule m = modules.findWithCourse(moduleId).orElseThrow(() -> ApiException.notFound("Módulo"));
        if (!m.getLessons().isEmpty()) {
            throw ApiException.business("MODULE_NOT_EMPTY", "Remova as aulas do módulo antes de excluí-lo.");
        }
        Course c = m.getCourse();
        c.getModules().remove(m);
        return view(c);
    }

    @Transactional
    public CourseAdminView addLesson(Long moduleId, LessonRequest req) {
        CourseModule m = modules.findWithCourse(moduleId).orElseThrow(() -> ApiException.notFound("Módulo"));
        validateQuiz(req.quizId());
        int next = m.getLessons().stream().mapToInt(Lesson::getPosition).max().orElse(0) + 1;
        Lesson l = new Lesson(m, next);
        l.update(req.title().trim(), req.content(), req.estimatedMinutes(), req.quizId());
        m.getLessons().add(l);
        modules.flush();
        return view(m.getCourse());
    }

    @Transactional
    public CourseAdminView updateLesson(Long lessonId, LessonRequest req) {
        Lesson l = lessons.findWithCourse(lessonId).orElseThrow(() -> ApiException.notFound("Aula"));
        validateQuiz(req.quizId());
        l.update(req.title().trim(), req.content(), req.estimatedMinutes(), req.quizId());
        return view(l.getModule().getCourse());
    }

    @Transactional
    public CourseAdminView deleteLesson(Long lessonId) {
        Lesson l = lessons.findWithCourse(lessonId).orElseThrow(() -> ApiException.notFound("Aula"));
        CourseModule m = l.getModule();
        m.getLessons().remove(l);
        return view(m.getCourse());
    }

    // ------------------------------------------------------------------ quizzes

    @Transactional(readOnly = true)
    public List<QuizAdminView> listQuizzes() {
        return quizzes.findAllByOrderByIdAsc().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public QuizAdminView getQuiz(Long id) {
        return view(quiz(id));
    }

    @Transactional
    public QuizAdminView createQuiz(QuizCreateRequest req) {
        validateQuestions(req.questions());
        Quiz q = new Quiz(time.now());
        applyMeta(q, req.quiz());
        q = quizzes.save(q);
        addQuestions(q, req.questions());
        quizzes.flush();
        return view(q);
    }

    @Transactional
    public QuizAdminView updateQuizMeta(Long id, QuizMetaRequest req) {
        Quiz q = quiz(id);
        applyMeta(q, req);
        return view(q);
    }

    /** Perguntas só podem ser substituídas enquanto ninguém respondeu o quiz (preserva o histórico). */
    @Transactional
    public QuizAdminView replaceQuestions(Long id, List<QuestionRequest> questions) {
        Quiz q = quiz(id);
        if (attempts.existsByQuizId(id)) {
            throw ApiException.conflict("QUIZ_HAS_ATTEMPTS",
                    "Este quiz já foi respondido. Crie um novo quiz e despublique este para preservar o histórico.");
        }
        validateQuestions(questions);
        q.getQuestions().clear();
        quizzes.flush();
        addQuestions(q, questions);
        quizzes.flush();
        return view(q);
    }

    @Transactional
    public void deleteQuiz(Long id) {
        Quiz q = quiz(id);
        if (attempts.existsByQuizId(id)) {
            throw ApiException.conflict("QUIZ_HAS_ATTEMPTS", "O quiz possui tentativas. Despublique-o em vez de excluir.");
        }
        lessons.findByQuizId(id).forEach(l -> l.update(l.getTitle(), l.getContent(), l.getEstimatedMinutes(), null));
        quizzes.delete(q);
    }

    static void validateQuestions(List<QuestionRequest> questions) {
        for (int i = 0; i < questions.size(); i++) {
            long correct = questions.get(i).options().stream().filter(AdminContentDtos.OptionRequest::correct).count();
            if (correct != 1) {
                throw ApiException.business("INVALID_QUESTION",
                        "A pergunta " + (i + 1) + " deve ter exatamente uma alternativa correta.");
            }
        }
    }

    private void addQuestions(Quiz q, List<QuestionRequest> questions) {
        int pos = 1;
        for (QuestionRequest qr : questions) {
            QuizQuestion question = new QuizQuestion(q, pos++, qr.statement().trim(), qr.explanation().trim(), qr.topic());
            int opt = 1;
            for (AdminContentDtos.OptionRequest o : qr.options()) {
                question.getOptions().add(new QuizOption(question, opt++, o.text().trim(), o.correct()));
            }
            q.getQuestions().add(question);
        }
    }

    private void applyMeta(Quiz q, QuizMetaRequest m) {
        q.update(m.title().trim(), m.description().trim(), m.category(), m.difficulty(), m.published(), time.now());
    }

    private void apply(Course c, CourseRequest r) {
        c.update(r.title().trim(), r.summary().trim(), r.description().trim(), r.objectives().trim(), r.category(),
                r.difficulty(), r.estimatedMinutes(), r.position(), r.published(), time.now());
    }

    private void validateQuiz(Long quizId) {
        if (quizId != null && !quizzes.existsById(quizId)) {
            throw ApiException.business("INVALID_QUIZ", "Quiz associado não encontrado.");
        }
    }

    private Course course(Long id) {
        return courses.findById(id).orElseThrow(() -> ApiException.notFound("Curso"));
    }

    private Quiz quiz(Long id) {
        return quizzes.findById(id).orElseThrow(() -> ApiException.notFound("Quiz"));
    }

    private CourseAdminView view(Course c) {
        List<ModuleAdminView> mods = c.getModules().stream().map(m -> new ModuleAdminView(m.getId(), m.getPosition(),
                m.getTitle(), m.getLessons().stream().map(l -> new LessonAdminView(l.getId(), l.getPosition(),
                        l.getTitle(), l.getContent(), l.getEstimatedMinutes(), l.getQuizId())).toList())).toList();
        return new CourseAdminView(c.getId(), c.getSlug(), c.getTitle(), c.getSummary(), c.getDescription(),
                c.getObjectives(), c.getCategory(), c.getDifficulty(), c.getEstimatedMinutes(), c.getPosition(),
                c.isPublished(), c.getId() == null ? 0 : enrollments.countByCourseId(c.getId()), mods);
    }

    private QuizAdminView view(Quiz q) {
        return new QuizAdminView(q.getId(), q.getTitle(), q.getDescription(), q.getCategory(), q.getDifficulty(),
                q.isPublished(), q.getId() != null && attempts.existsByQuizId(q.getId()),
                q.getQuestions().stream().map(x -> new QuestionAdminView(x.getId(), x.getPosition(), x.getStatement(),
                        x.getExplanation(), x.getTopic(), x.getOptions().stream()
                        .map(o -> new OptionAdminView(o.getId(), o.getText(), o.isCorrect())).toList())).toList());
    }
}

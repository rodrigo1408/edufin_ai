package br.finedu.course;

import br.finedu.common.ApiException;
import br.finedu.common.AppTime;
import br.finedu.common.ContentPolicy;
import br.finedu.common.Difficulty;
import br.finedu.common.PageResponse;
import br.finedu.common.Topic;
import br.finedu.course.CourseDtos.CompletionResult;
import br.finedu.course.CourseDtos.CourseCard;
import br.finedu.course.CourseDtos.CourseDetail;
import br.finedu.course.CourseDtos.EnrollmentView;
import br.finedu.course.CourseDtos.LearningItem;
import br.finedu.course.CourseDtos.LessonSummary;
import br.finedu.course.CourseDtos.LessonView;
import br.finedu.course.CourseDtos.ModuleView;
import br.finedu.course.CourseDtos.MyReview;
import br.finedu.course.CourseDtos.NoteView;
import br.finedu.course.CourseDtos.ReviewRequest;
import br.finedu.course.CourseDtos.ReviewView;
import br.finedu.gamification.GamificationService;
import br.finedu.gamification.GamificationService.Reward;
import br.finedu.gamification.PointRule;
import br.finedu.notification.NotificationService;
import br.finedu.notification.NotificationType;
import br.finedu.user.ActivityLog;
import br.finedu.user.ActivityService;
import br.finedu.user.User;
import br.finedu.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courses;
    private final LessonRepository lessons;
    private final EnrollmentRepository enrollments;
    private final LessonProgressRepository progress;
    private final LessonNoteRepository notes;
    private final CourseReviewRepository reviews;
    private final UserRepository users;
    private final GamificationService gamification;
    private final NotificationService notifications;
    private final ActivityService activity;
    private final AppTime time;

    public CourseService(CourseRepository courses, LessonRepository lessons, EnrollmentRepository enrollments,
                         LessonProgressRepository progress, LessonNoteRepository notes, CourseReviewRepository reviews,
                         UserRepository users, GamificationService gamification, NotificationService notifications,
                         ActivityService activity, AppTime time) {
        this.courses = courses;
        this.lessons = lessons;
        this.enrollments = enrollments;
        this.progress = progress;
        this.notes = notes;
        this.reviews = reviews;
        this.users = users;
        this.gamification = gamification;
        this.notifications = notifications;
        this.activity = activity;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public List<CourseCard> catalog(Long userId, Topic topic, Difficulty difficulty) {
        List<Course> list = courses.findPublished(topic, difficulty);
        Map<Long, Enrollment> enrolled = enrollments.findByUserIdOrderByEnrolledAtDesc(userId).stream()
                .collect(Collectors.toMap(Enrollment::getCourseId, Function.identity()));
        Map<Long, Long> done = completedByCourse(userId);
        Map<Long, Object[]> ratings = ratingStats();
        return list.stream().map(c -> card(c, enrolled.get(c.getId()), done.getOrDefault(c.getId(), 0L), ratings))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseDetail detail(Long userId, Long courseId) {
        Course c = courses.findByIdAndPublishedTrue(courseId).orElseThrow(() -> ApiException.notFound("Curso"));
        Optional<Enrollment> enrollment = enrollments.findByUserIdAndCourseId(userId, courseId);
        List<Lesson> ordered = c.orderedLessons();
        Set<Long> completed = completedLessonIds(userId, ordered);
        List<ModuleView> modules = c.getModules().stream()
                .map(m -> new ModuleView(m.getId(), m.getTitle(), m.getLessons().stream()
                        .map(l -> new LessonSummary(l.getId(), l.getTitle(), l.getEstimatedMinutes(),
                                completed.contains(l.getId()), l.getQuizId() != null))
                        .toList()))
                .toList();
        EnrollmentView ev = enrollment.map(e -> new EnrollmentView(e.getEnrolledAt(), e.getCompletedAt(),
                e.getLastLessonId(), nextLesson(ordered, completed, e.getLastLessonId()))).orElse(null);
        MyReview myReview = reviews.findByUserIdAndCourseId(userId, courseId)
                .map(r -> new MyReview(r.getRating(), r.getComment())).orElse(null);
        List<String> objectives = Arrays.stream(c.getObjectives().split("\\R")).map(String::strip)
                .filter(s -> !s.isEmpty()).toList();
        return new CourseDetail(card(c, enrollment.orElse(null), completed.size(), ratingStats()), c.getDescription(),
                objectives, modules, ev, myReview);
    }

    @Transactional
    public CourseDetail enroll(Long userId, Long courseId) {
        Course c = courses.findByIdAndPublishedTrue(courseId).orElseThrow(() -> ApiException.notFound("Curso"));
        if (!enrollments.existsByUserIdAndCourseId(userId, courseId)) {
            enrollments.save(new Enrollment(userId, courseId, time.now()));
            activity.record(userId, ActivityLog.Type.COURSE_ENROLLED, "Inscrição no curso " + c.getTitle());
        }
        return detail(userId, courseId);
    }

    @Transactional(readOnly = true)
    public LessonView lesson(Long userId, Long lessonId) {
        Lesson lesson = publishedLesson(lessonId);
        Course course = lesson.getModule().getCourse();
        requireEnrollment(userId, course.getId());
        List<Lesson> ordered = course.orderedLessons();
        int idx = indexOf(ordered, lessonId);
        String note = notes.findByUserIdAndLessonId(userId, lessonId).map(LessonNote::getContent).orElse(null);
        return new LessonView(lesson.getId(), lesson.getTitle(), lesson.getContent(), lesson.getEstimatedMinutes(),
                course.getId(), course.getTitle(), lesson.getModule().getTitle(),
                progress.existsByUserIdAndLessonId(userId, lessonId), note, lesson.getQuizId(),
                idx > 0 ? ordered.get(idx - 1).getId() : null,
                idx < ordered.size() - 1 ? ordered.get(idx + 1).getId() : null, idx + 1, ordered.size());
    }

    /** Registra a última aula aberta para permitir "continuar de onde parou". */
    @Transactional
    public void visit(Long userId, Long lessonId) {
        Lesson lesson = publishedLesson(lessonId);
        requireEnrollment(userId, lesson.getModule().getCourse().getId()).touch(lessonId);
    }

    @Transactional
    public CompletionResult completeLesson(Long userId, Long lessonId) {
        Lesson lesson = publishedLesson(lessonId);
        Course course = lesson.getModule().getCourse();
        Enrollment enrollment = requireEnrollment(userId, course.getId());
        enrollment.touch(lessonId);
        Reward reward = Reward.none();
        if (!progress.existsByUserIdAndLessonId(userId, lessonId)) {
            progress.save(new LessonProgress(userId, lessonId, time.now()));
            activity.record(userId, ActivityLog.Type.LESSON_COMPLETED, "Aula concluída: " + lesson.getTitle());
            reward = gamification.award(userId, PointRule.LESSON_COMPLETED, String.valueOf(lessonId));
        }
        List<Lesson> ordered = course.orderedLessons();
        Set<Long> completed = completedLessonIds(userId, ordered);
        completed.add(lessonId);
        boolean courseCompleted = false;
        if (completed.size() == ordered.size() && enrollment.complete(time.now())) {
            courseCompleted = true;
            activity.record(userId, ActivityLog.Type.COURSE_COMPLETED, "Curso concluído: " + course.getTitle());
            notifications.notify(userId, NotificationType.COURSE_COMPLETED, "Curso concluído! 🎓",
                    "Você concluiu o curso \"" + course.getTitle() + "\". Que tal avaliar o curso?",
                    "/cursos/" + course.getId(), "COURSE_COMPLETED:" + course.getId());
            reward = reward.plus(gamification.award(userId, PointRule.COURSE_COMPLETED, String.valueOf(course.getId())));
        }
        return new CompletionResult(courseCompleted, percent(completed.size(), ordered.size()),
                nextLesson(ordered, completed, lessonId), reward);
    }

    @Transactional
    public NoteView saveNote(Long userId, Long lessonId, String content) {
        Lesson lesson = publishedLesson(lessonId);
        Course course = lesson.getModule().getCourse();
        requireEnrollment(userId, course.getId());
        String clean = ContentPolicy.sanitize(content);
        Optional<LessonNote> existing = notes.findByUserIdAndLessonId(userId, lessonId);
        if (clean == null || clean.isEmpty()) {
            existing.ifPresent(notes::delete);
            return new NoteView(lessonId, lesson.getTitle(), course.getId(), course.getTitle(), null, null);
        }
        LessonNote note = existing.orElseGet(() -> new LessonNote(userId, lessonId));
        note.write(clean, time.now());
        notes.save(note);
        return new NoteView(lessonId, lesson.getTitle(), course.getId(), course.getTitle(), note.getContent(),
                note.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public List<NoteView> myNotes(Long userId) {
        return notes.findByUserIdOrderByUpdatedAtDesc(userId).stream().map(n -> {
            Lesson l = lessons.findWithCourse(n.getLessonId()).orElseThrow();
            Course c = l.getModule().getCourse();
            return new NoteView(l.getId(), l.getTitle(), c.getId(), c.getTitle(), n.getContent(), n.getUpdatedAt());
        }).toList();
    }

    /** Avaliação do curso: apenas inscritos; uma por pessoa (pode ser editada). */
    @Transactional
    public MyReview review(Long userId, Long courseId, ReviewRequest req) {
        courses.findByIdAndPublishedTrue(courseId).orElseThrow(() -> ApiException.notFound("Curso"));
        requireEnrollment(userId, courseId);
        String comment = ContentPolicy.check(req.comment());
        CourseReview r = reviews.findByUserIdAndCourseId(userId, courseId)
                .orElseGet(() -> new CourseReview(userId, courseId, time.now()));
        r.update(req.rating(), comment == null || comment.isEmpty() ? null : comment, time.now());
        reviews.save(r);
        return new MyReview(r.getRating(), r.getComment());
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewView> reviews(Long courseId, int page, int size) {
        Page<CourseReview> p = reviews.findByCourseIdAndHiddenFalseOrderByCreatedAtDesc(courseId, PageRequest.of(page, size));
        Map<Long, String> names = users.findAllById(p.getContent().stream().map(CourseReview::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, User::getDisplayName));
        return PageResponse.of(p, r -> new ReviewView(r.getId(), names.getOrDefault(r.getUserId(), "Usuário"),
                r.getRating(), r.getComment(), r.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public List<LearningItem> myLearning(Long userId) {
        Map<Long, Long> done = completedByCourse(userId);
        return enrollments.findByUserIdOrderByEnrolledAtDesc(userId).stream()
                .map(e -> courses.findById(e.getCourseId()).map(c -> {
                    List<Lesson> ordered = c.orderedLessons();
                    Set<Long> completed = completedLessonIds(userId, ordered);
                    return new LearningItem(c.getId(), c.getTitle(), c.getCategory(),
                            percent(done.getOrDefault(c.getId(), 0L).intValue(), ordered.size()),
                            completed.size(), ordered.size(), nextLesson(ordered, completed, e.getLastLessonId()),
                            e.getEnrolledAt(), e.getCompletedAt());
                }).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseCard> recommendedFor(Long userId, Set<Topic> topics, int limit) {
        if (topics.isEmpty()) {
            return List.of();
        }
        Map<Long, Object[]> ratings = ratingStats();
        Map<Long, Long> done = completedByCourse(userId);
        Map<Long, Enrollment> enrolled = enrollments.findByUserIdOrderByEnrolledAtDesc(userId).stream()
                .collect(Collectors.toMap(Enrollment::getCourseId, Function.identity()));
        return courses.findByPublishedTrueAndCategoryInOrderByPositionAsc(topics).stream()
                .filter(c -> enrolled.get(c.getId()) == null || enrolled.get(c.getId()).getCompletedAt() == null)
                .limit(limit)
                .map(c -> card(c, enrolled.get(c.getId()), done.getOrDefault(c.getId(), 0L), ratings))
                .toList();
    }

    private Lesson publishedLesson(Long lessonId) {
        Lesson lesson = lessons.findWithCourse(lessonId).orElseThrow(() -> ApiException.notFound("Aula"));
        if (!lesson.getModule().getCourse().isPublished()) {
            throw ApiException.notFound("Aula");
        }
        return lesson;
    }

    private Enrollment requireEnrollment(Long userId, Long courseId) {
        return enrollments.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> ApiException.forbidden("Inscreva-se no curso para acessar as aulas."));
    }

    private Set<Long> completedLessonIds(Long userId, List<Lesson> ordered) {
        if (ordered.isEmpty()) {
            return new HashSet<>();
        }
        return progress.findByUserIdAndLessonIdIn(userId, ordered.stream().map(Lesson::getId).toList()).stream()
                .map(LessonProgress::getLessonId).collect(Collectors.toCollection(HashSet::new));
    }

    private Map<Long, Long> completedByCourse(Long userId) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : progress.completedCountByCourse(userId)) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private Map<Long, Object[]> ratingStats() {
        Map<Long, Object[]> map = new HashMap<>();
        for (Object[] row : reviews.ratingStats()) {
            map.put((Long) row[0], row);
        }
        return map;
    }

    private CourseCard card(Course c, Enrollment e, long completedLessons, Map<Long, Object[]> ratings) {
        int total = c.orderedLessons().size();
        Object[] stats = ratings.get(c.getId());
        BigDecimal avg = stats == null ? null
                : BigDecimal.valueOf(((Number) stats[1]).doubleValue()).setScale(1, RoundingMode.HALF_EVEN);
        long count = stats == null ? 0 : (Long) stats[2];
        return new CourseCard(c.getId(), c.getSlug(), c.getTitle(), c.getSummary(), c.getCategory(),
                c.getCategory().label(), c.getDifficulty(), c.getEstimatedMinutes(), c.getPosition(), total, avg, count,
                e != null, e != null && e.getCompletedAt() != null, (int) completedLessons,
                percent((int) completedLessons, total));
    }

    static BigDecimal percent(int done, int total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_EVEN);
        }
        return BigDecimal.valueOf(Math.min(done, total) * 100L).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_EVEN);
    }

    /** Próxima aula não concluída após a referência (ou a primeira pendente do curso). */
    static Long nextLesson(List<Lesson> ordered, Set<Long> completed, Long reference) {
        int start = reference == null ? 0 : Math.max(0, indexOf(ordered, reference));
        for (int i = start; i < ordered.size(); i++) {
            if (!completed.contains(ordered.get(i).getId())) {
                return ordered.get(i).getId();
            }
        }
        for (Lesson l : ordered) {
            if (!completed.contains(l.getId())) {
                return l.getId();
            }
        }
        return null;
    }

    private static int indexOf(List<Lesson> ordered, Long lessonId) {
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(lessonId)) {
                return i;
            }
        }
        return -1;
    }
}

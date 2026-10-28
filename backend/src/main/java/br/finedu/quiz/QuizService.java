package br.finedu.quiz;

import br.finedu.common.ApiException;
import br.finedu.common.AppTime;
import br.finedu.common.Difficulty;
import br.finedu.common.PageResponse;
import br.finedu.common.Topic;
import br.finedu.course.CourseService;
import br.finedu.gamification.GamificationService;
import br.finedu.gamification.GamificationService.Reward;
import br.finedu.gamification.PointRule;
import br.finedu.quiz.QuizDtos.AnswerFeedback;
import br.finedu.quiz.QuizDtos.AnswerRequest;
import br.finedu.quiz.QuizDtos.AttemptHistoryItem;
import br.finedu.quiz.QuizDtos.AttemptResult;
import br.finedu.quiz.QuizDtos.AttemptView;
import br.finedu.quiz.QuizDtos.OptionView;
import br.finedu.quiz.QuizDtos.QuestionView;
import br.finedu.quiz.QuizDtos.QuizCard;
import br.finedu.quiz.QuizDtos.QuizDetail;
import br.finedu.user.ActivityLog;
import br.finedu.user.ActivityService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Execução de quizzes. O cliente nunca recebe a alternativa correta antes de responder; a
 * correção e a pontuação são calculadas exclusivamente a partir das respostas persistidas.
 */
@Service
public class QuizService {

    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;
    private final QuizAttemptAnswerRepository answers;
    private final CourseService courses;
    private final GamificationService gamification;
    private final ActivityService activity;
    private final AppTime time;

    public QuizService(QuizRepository quizzes, QuizAttemptRepository attempts, QuizAttemptAnswerRepository answers,
                       CourseService courses, GamificationService gamification, ActivityService activity,
                       AppTime time) {
        this.quizzes = quizzes;
        this.attempts = attempts;
        this.answers = answers;
        this.courses = courses;
        this.gamification = gamification;
        this.activity = activity;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public List<QuizCard> list(Long userId, Topic topic, Difficulty difficulty) {
        Map<Long, Object[]> best = bestByQuiz(userId);
        return quizzes.findPublished(topic, difficulty).stream().map(q -> card(q, best.get(q.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public QuizDetail detail(Long userId, Long quizId) {
        Quiz quiz = published(quizId);
        List<AttemptHistoryItem> mine = attempts.findByUserIdAndQuizIdOrderByStartedAtDesc(userId, quizId).stream()
                .filter(a -> a.getStatus() == QuizAttempt.Status.COMPLETED)
                .map(a -> historyItem(a, quiz)).toList();
        Long inProgress = attempts.findFirstByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(userId, quizId,
                QuizAttempt.Status.IN_PROGRESS).map(QuizAttempt::getId).orElse(null);
        return new QuizDetail(card(quiz, bestByQuiz(userId).get(quizId)), mine, inProgress);
    }

    /** Inicia uma tentativa ou retoma a que estiver em andamento para o mesmo quiz. */
    @Transactional
    public AttemptView start(Long userId, Long quizId) {
        Quiz quiz = published(quizId);
        if (quiz.getQuestions().isEmpty()) {
            throw ApiException.business("EMPTY_QUIZ", "Este quiz ainda não possui perguntas.");
        }
        QuizAttempt attempt = attempts.findFirstByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(userId, quizId,
                        QuizAttempt.Status.IN_PROGRESS)
                .orElseGet(() -> attempts.save(new QuizAttempt(userId, quizId, quiz.getQuestions().size(), time.now())));
        return attemptView(attempt, quiz);
    }

    @Transactional(readOnly = true)
    public AttemptView getAttempt(Long userId, Long attemptId) {
        QuizAttempt attempt = attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> ApiException.notFound("Tentativa"));
        return attemptView(attempt, quizzes.findById(attempt.getQuizId()).orElseThrow());
    }

    /**
     * Registra a resposta de uma pergunta (uma única vez) e devolve a correção e a explicação
     * daquela pergunta. Respostas repetidas são rejeitadas.
     */
    @Transactional
    public AnswerFeedback answer(Long userId, Long attemptId, AnswerRequest req) {
        QuizAttempt attempt = attempts.lockByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> ApiException.notFound("Tentativa"));
        if (attempt.getStatus() != QuizAttempt.Status.IN_PROGRESS) {
            throw ApiException.business("ATTEMPT_FINISHED", "Esta tentativa já foi finalizada.");
        }
        Quiz quiz = quizzes.findById(attempt.getQuizId()).orElseThrow();
        QuizQuestion question = quiz.getQuestions().stream().filter(q -> q.getId().equals(req.questionId()))
                .findFirst().orElseThrow(() -> ApiException.business("INVALID_QUESTION", "Pergunta não pertence a este quiz."));
        QuizOption option = question.getOptions().stream().filter(o -> o.getId().equals(req.optionId()))
                .findFirst().orElseThrow(() -> ApiException.business("INVALID_OPTION", "Alternativa inválida para esta pergunta."));
        if (answers.existsByAttemptIdAndQuestionId(attemptId, question.getId())) {
            throw ApiException.conflict("ALREADY_ANSWERED", "Esta pergunta já foi respondida nesta tentativa.");
        }
        answers.save(new QuizAttemptAnswer(attemptId, question.getId(), option.getId(), option.isCorrect(), time.now()));
        return feedback(question, option.getId(), option.isCorrect());
    }

    /** Finaliza a tentativa; a nota considera somente as respostas registradas no servidor. */
    @Transactional
    public AttemptResult finish(Long userId, Long attemptId) {
        QuizAttempt attempt = attempts.lockByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> ApiException.notFound("Tentativa"));
        if (attempt.getStatus() != QuizAttempt.Status.IN_PROGRESS) {
            throw ApiException.business("ATTEMPT_FINISHED", "Esta tentativa já foi finalizada.");
        }
        Quiz quiz = quizzes.findById(attempt.getQuizId()).orElseThrow();
        int correct = (int) answers.countByAttemptIdAndCorrectTrue(attemptId);
        QuizScoring.Score score = QuizScoring.score(correct, attempt.getTotalQuestions());
        attempt.complete(score, time.now());
        activity.record(userId, ActivityLog.Type.QUIZ_COMPLETED,
                "Quiz \"" + quiz.getTitle() + "\": " + correct + "/" + score.total() + " acertos");

        Reward reward = gamification.award(userId, PointRule.QUIZ_FINISHED, String.valueOf(attemptId));
        if (score.passed()) {
            reward = reward.plus(gamification.award(userId, PointRule.QUIZ_FIRST_PASS, String.valueOf(quiz.getId())));
        }
        if (score.perfect()) {
            reward = reward.plus(gamification.award(userId, PointRule.QUIZ_PERFECT, String.valueOf(quiz.getId())));
        }
        return result(attempt, quiz, reward);
    }

    @Transactional(readOnly = true)
    public AttemptResult result(Long userId, Long attemptId) {
        QuizAttempt attempt = attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> ApiException.notFound("Tentativa"));
        if (attempt.getStatus() != QuizAttempt.Status.COMPLETED) {
            throw ApiException.business("ATTEMPT_IN_PROGRESS", "Finalize a tentativa para ver o resultado.");
        }
        return result(attempt, quizzes.findById(attempt.getQuizId()).orElseThrow(), Reward.none());
    }

    @Transactional(readOnly = true)
    public PageResponse<AttemptHistoryItem> history(Long userId, int page, int size) {
        Map<Long, Quiz> cache = new HashMap<>();
        return PageResponse.of(attempts.findByUserIdAndStatusOrderByFinishedAtDesc(userId, QuizAttempt.Status.COMPLETED,
                        PageRequest.of(page, size)),
                a -> historyItem(a, cache.computeIfAbsent(a.getQuizId(), id -> quizzes.findById(id).orElseThrow())));
    }

    private AttemptResult result(QuizAttempt attempt, Quiz quiz, Reward reward) {
        Map<Long, QuizAttemptAnswer> byQuestion = answers.findByAttemptId(attempt.getId()).stream()
                .collect(Collectors.toMap(QuizAttemptAnswer::getQuestionId, Function.identity()));
        Set<Topic> weak = new LinkedHashSet<>();
        List<AnswerFeedback> review = quiz.getQuestions().stream().map(q -> {
            QuizAttemptAnswer a = byQuestion.get(q.getId());
            boolean correct = a != null && a.isCorrect();
            if (!correct) {
                weak.add(q.getTopic());
            }
            return feedback(q, a == null ? null : a.getOptionId(), correct);
        }).toList();
        QuizScoring.Score score = QuizScoring.score(attempt.getCorrectCount(), attempt.getTotalQuestions());
        return new AttemptResult(attempt.getId(), quiz.getId(), quiz.getTitle(), score.correct(), score.total(),
                score.points(), score.percent(), score.passed(), attempt.getFinishedAt(), review,
                weak.stream().map(Topic::label).toList(), courses.recommendedFor(attempt.getUserId(), weak, 3), reward);
    }

    private AttemptView attemptView(QuizAttempt attempt, Quiz quiz) {
        List<QuestionView> questions = quiz.getQuestions().stream()
                .map(q -> new QuestionView(q.getId(), q.getPosition(), q.getStatement(),
                        q.getOptions().stream().map(o -> new OptionView(o.getId(), o.getText())).toList()))
                .toList();
        Map<Long, QuizQuestion> byId = quiz.getQuestions().stream()
                .collect(Collectors.toMap(QuizQuestion::getId, Function.identity()));
        List<AnswerFeedback> answered = answers.findByAttemptId(attempt.getId()).stream()
                .map(a -> feedback(byId.get(a.getQuestionId()), a.getOptionId(), a.isCorrect())).toList();
        return new AttemptView(attempt.getId(), quiz.getId(), quiz.getTitle(), attempt.getStatus(), questions, answered);
    }

    private static AnswerFeedback feedback(QuizQuestion q, Long selected, boolean correct) {
        return new AnswerFeedback(q.getId(), selected, correct,
                q.correctOption().map(QuizOption::getId).orElse(null), q.getExplanation());
    }

    private Quiz published(Long quizId) {
        return quizzes.findByIdAndPublishedTrue(quizId).orElseThrow(() -> ApiException.notFound("Quiz"));
    }

    private Map<Long, Object[]> bestByQuiz(Long userId) {
        Map<Long, Object[]> map = new HashMap<>();
        for (Object[] row : attempts.bestByQuiz(userId, QuizAttempt.Status.COMPLETED)) {
            map.put((Long) row[0], row);
        }
        return map;
    }

    private static QuizCard card(Quiz q, Object[] best) {
        return new QuizCard(q.getId(), q.getTitle(), q.getDescription(), q.getCategory(), q.getCategory().label(),
                q.getDifficulty(), q.getQuestions().size(), best == null ? null : (BigDecimal) best[1],
                best == null ? 0 : (Long) best[2]);
    }

    private static AttemptHistoryItem historyItem(QuizAttempt a, Quiz quiz) {
        return new AttemptHistoryItem(a.getId(), quiz.getId(), quiz.getTitle(), quiz.getCategory(),
                a.getCorrectCount() == null ? 0 : a.getCorrectCount(), a.getTotalQuestions(), a.getPercent(),
                a.getFinishedAt());
    }
}

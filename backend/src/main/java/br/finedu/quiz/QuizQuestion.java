package br.finedu.quiz;

import br.finedu.common.Topic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 500)
    private String statement;

    @Column(nullable = false, length = 1000)
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Topic topic;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<QuizOption> options = new ArrayList<>();

    protected QuizQuestion() {
    }

    public QuizQuestion(Quiz quiz, int position, String statement, String explanation, Topic topic) {
        this.quiz = quiz;
        this.position = position;
        this.statement = statement;
        this.explanation = explanation;
        this.topic = topic;
    }

    public Optional<QuizOption> correctOption() {
        return options.stream().filter(QuizOption::isCorrect).findFirst();
    }

    public Long getId() {
        return id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public int getPosition() {
        return position;
    }

    public String getStatement() {
        return statement;
    }

    public String getExplanation() {
        return explanation;
    }

    public Topic getTopic() {
        return topic;
    }

    public List<QuizOption> getOptions() {
        return options;
    }
}

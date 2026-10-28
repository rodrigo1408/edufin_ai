package br.finedu.quiz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Alternativa. O indicador {@code correct} nunca é enviado ao cliente durante a execução do quiz. */
@Entity
@Table(name = "quiz_options")
public class QuizOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private QuizQuestion question;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 300)
    private String text;

    @Column(nullable = false)
    private boolean correct;

    protected QuizOption() {
    }

    public QuizOption(QuizQuestion question, int position, String text, boolean correct) {
        this.question = question;
        this.position = position;
        this.text = text;
        this.correct = correct;
    }

    public Long getId() {
        return id;
    }

    public QuizQuestion getQuestion() {
        return question;
    }

    public int getPosition() {
        return position;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }
}

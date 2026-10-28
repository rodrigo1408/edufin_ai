package br.finedu.forum;

/** Enums compartilhados do módulo de comunidade. */
public final class ForumEntities {

    private ForumEntities() {
    }

    public enum Category { QUESTIONS, TIPS, GOALS, STUDIES, INVESTING, PLATFORM }

    /** VISIBLE: público; HIDDEN: ocultado (pela moderação ou por denúncias), reversível; REMOVED: removido. */
    public enum Status { VISIBLE, HIDDEN, REMOVED }

    public enum ReportReason { SPAM, OFFENSIVE, PERSONAL_DATA, SCAM, BULLYING, OTHER }

    public enum ReportStatus { OPEN, RESOLVED, DISMISSED }
}

package br.finedu.transaction;

/** Categorias fixas, cada uma válida para um único tipo (receita ou despesa). */
public enum TransactionCategory {
    ALLOWANCE(Transaction.Type.INCOME, "Salário ou mesada"),
    WORK(Transaction.Type.INCOME, "Trabalho / bico"),
    GIFT_RECEIVED(Transaction.Type.INCOME, "Presente recebido"),
    SALE(Transaction.Type.INCOME, "Venda"),
    OTHER_INCOME(Transaction.Type.INCOME, "Outras receitas"),
    FOOD(Transaction.Type.EXPENSE, "Alimentação"),
    TRANSPORT(Transaction.Type.EXPENSE, "Transporte"),
    LEISURE(Transaction.Type.EXPENSE, "Lazer"),
    EDUCATION(Transaction.Type.EXPENSE, "Educação"),
    CLOTHING(Transaction.Type.EXPENSE, "Roupas"),
    TECHNOLOGY(Transaction.Type.EXPENSE, "Tecnologia e jogos"),
    HEALTH(Transaction.Type.EXPENSE, "Saúde"),
    SUBSCRIPTIONS(Transaction.Type.EXPENSE, "Assinaturas"),
    GIFTS_GIVEN(Transaction.Type.EXPENSE, "Presentes"),
    OTHER_EXPENSE(Transaction.Type.EXPENSE, "Outras despesas");

    private final Transaction.Type type;
    private final String label;

    TransactionCategory(Transaction.Type type, String label) {
        this.type = type;
        this.label = label;
    }

    public Transaction.Type type() {
        return type;
    }

    public String label() {
        return label;
    }
}

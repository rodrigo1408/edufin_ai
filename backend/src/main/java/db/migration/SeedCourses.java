package db.migration;

import db.migration.V2__SeedContent.CourseSeed;
import db.migration.V2__SeedContent.LessonSeed;
import db.migration.V2__SeedContent.ModuleSeed;
import java.util.List;

/**
 * Trilha inicial, em ordem progressiva: orçamento → poupança/metas → consumo consciente →
 * juros/inflação → crédito/dívidas → investimentos. Conteúdo em Markdown simples.
 */
final class SeedCourses {

    private SeedCourses() {
    }

    static final List<CourseSeed> ALL = List.of(
            new CourseSeed("primeiros-passos", "Primeiros passos com o dinheiro",
                    "Entenda para que serve o dinheiro e monte seu primeiro orçamento.",
                    "Um curso curto e prático para quem está começando. Você vai entender a diferença entre necessidades e desejos, aprender a montar um orçamento simples e a registrar o que entra e o que sai.",
                    """
                    Explicar para que serve o dinheiro e por que planejar
                    Diferenciar necessidades e desejos
                    Montar um orçamento simples com a regra 50-30-20 como referência
                    Registrar receitas e despesas de forma consistente
                    """, "BUDGET", "BEGINNER", 35, List.of(
                    new ModuleSeed("Dinheiro e você", List.of(
                            new LessonSeed("Para que serve o dinheiro", 7, null, """
                                    ## Dinheiro é uma ferramenta

                                    O dinheiro é uma forma de **trocar o seu tempo e esforço** (ou o de alguém da sua família) por coisas de que você precisa ou que deseja. Ele não é um fim em si: é um meio para realizar planos.

                                    ## Três coisas que dá para fazer com dinheiro

                                    - **Gastar**: comprar algo agora.
                                    - **Guardar**: reservar para um objetivo futuro ou para imprevistos.
                                    - **Doar ou compartilhar**: ajudar alguém ou uma causa.

                                    Toda vez que você recebe dinheiro, está decidindo — mesmo sem perceber — quanto vai para cada uma dessas opções.

                                    > Dica: decidir antes de receber é muito mais fácil do que decidir com o dinheiro na mão.

                                    ## Por que planejar?

                                    Sem plano, o dinheiro costuma \"sumir\" em pequenos gastos. Com plano, você consegue comprar aquilo que realmente importa para você, sem sustos. Nas próximas aulas você vai aprender a fazer isso na prática.
                                    """),
                            new LessonSeed("Necessidades x desejos", 8, null, """
                                    ## Necessidade

                                    É aquilo que você precisa para viver, estudar e se manter saudável: alimentação, transporte para a escola, material escolar, saúde.

                                    ## Desejo

                                    É aquilo que seria bom ter, mas dá para esperar ou viver sem: o tênis da moda, a skin nova, o fone mais caro.

                                    ## Desejo não é vilão

                                    Querer coisas é normal! O problema é quando os desejos ocupam o espaço das necessidades ou viram dívida. A saída é **planejar os desejos**: transformá-los em metas, com valor e prazo.

                                    ## Teste rápido

                                    Antes de comprar, pergunte:

                                    1. Eu preciso disso agora ou só quero?
                                    2. O que acontece se eu esperar uma semana?
                                    3. Esse dinheiro faria falta para algo mais importante?

                                    > Se a vontade passar depois de alguns dias, provavelmente era impulso.
                                    """))),
                    new ModuleSeed("Orçamento na prática", List.of(
                            new LessonSeed("Montando seu primeiro orçamento", 10, null, """
                                    ## Passo a passo

                                    1. **Anote o que entra**: mesada, bicos, presentes em dinheiro.
                                    2. **Liste os gastos fixos**: os que se repetem todo mês (ex.: transporte, lanche).
                                    3. **Estime os gastos variáveis**: lazer, jogos, roupas.
                                    4. **Defina quanto vai guardar** — e separe esse valor primeiro.
                                    5. **Compare o previsto com o realizado** no fim do mês.

                                    ## A regra 50-30-20 (como referência)

                                    - 50% para necessidades
                                    - 30% para desejos
                                    - 20% para guardar ou investir

                                    Se a sua família já cobre as necessidades, você pode guardar uma fatia maior. Adapte à sua realidade.

                                    ## Exemplo

                                    Recebendo R$ 200 por mês: R$ 100 para necessidades, R$ 60 para desejos e R$ 40 para a sua meta. Em 6 meses, são R$ 240 guardados.

                                    > \"Pague-se primeiro\": guarde no momento em que recebe, não com o que sobrar.
                                    """),
                            new LessonSeed("Registrando entradas e saídas", 10, "budget", """
                                    ## Por que registrar?

                                    Registrar é o que transforma o orçamento em realidade. Sem registro, você só **acha** que sabe para onde o dinheiro vai.

                                    ## Como fazer na plataforma

                                    - Use **Transações** para lançar receitas (mesada, trabalho) e despesas (lanche, transporte).
                                    - Escolha a **categoria** certa — ela alimenta os gráficos de Relatórios.
                                    - Se for só um exercício, marque como **simulado**.

                                    ## Hábito de 2 minutos

                                    Reserve dois minutos por dia (ou logo depois de cada compra) para registrar. No fim da semana, olhe o resumo: onde você gastou mais? Era o que você planejou?

                                    ## Revisão mensal

                                    1. Quanto entrou?
                                    2. Quanto saiu, e em quais categorias?
                                    3. Você guardou o que planejou?
                                    4. O que vai ajustar no próximo mês?

                                    Ao terminar esta aula, faça o quiz **Orçamento pessoal na prática** para fixar o conteúdo.
                                    """))))),
            new CourseSeed("poupanca-e-metas", "Poupança e metas",
                    "Aprenda a guardar dinheiro com propósito e a criar metas que funcionam.",
                    "Guardar dinheiro fica mais fácil quando existe um motivo. Neste curso você vai entender a reserva de emergência, criar metas bem definidas e conhecer estratégias simples para poupar com constância.",
                    """
                    Explicar a função da reserva de emergência
                    Criar metas específicas, com valor e prazo
                    Calcular quanto guardar por mês para atingir uma meta
                    Aplicar estratégias para poupar com constância
                    """, "SAVING", "BEGINNER", 40, List.of(
                    new ModuleSeed("Por que guardar", List.of(
                            new LessonSeed("O poder de poupar", 8, null, """
                                    ## Poupar é escolher o futuro

                                    Poupar é abrir mão de um pouco do consumo de hoje para ter mais liberdade amanhã: comprar algo maior, lidar com imprevistos ou começar a investir.

                                    ## Pequenos valores, grande diferença

                                    Guardar R$ 5 por dia parece pouco, mas em um ano são mais de **R$ 1.800**. A constância importa mais do que o valor.

                                    ## Onde guardar?

                                    O importante é um lugar seguro, separado do dinheiro do dia a dia. Converse com seus responsáveis sobre opções como uma conta digital para menores ou um investimento conservador acompanhado por eles.

                                    > Dinheiro guardado \"na carteira\" costuma ser gasto. Separe!
                                    """),
                            new LessonSeed("Reserva de emergência", 8, null, """
                                    ## O que é

                                    Um dinheiro guardado **só para imprevistos**: o celular que quebrou, a passagem extra, um conserto.

                                    ## Quanto guardar?

                                    Para adultos, fala-se em 3 a 6 meses de gastos essenciais. Para você, um bom começo é o equivalente a **um ou dois meses de mesada**.

                                    ## Regras da reserva

                                    - Não é para desejos — nem para promoções \"imperdíveis\".
                                    - Deve ficar em local seguro e fácil de resgatar (alta **liquidez**).
                                    - Usou? Reponha assim que possível.

                                    Que tal criar uma meta chamada \"Reserva de emergência\" na plataforma?
                                    """))),
                    new ModuleSeed("Metas que funcionam", List.of(
                            new LessonSeed("Metas específicas, com valor e prazo", 10, null, """
                                    ## Meta vaga x meta clara

                                    - Vaga: \"quero juntar dinheiro\".
                                    - Clara: \"juntar **R$ 600** para um **fone** até **dezembro**\".

                                    Uma boa meta responde: **o quê, quanto e até quando**.

                                    ## Fazendo a conta

                                    Divida o valor que falta pelo número de meses até o prazo:

                                    > R$ 600 ÷ 6 meses = R$ 100 por mês

                                    Se o valor mensal ficar pesado, você pode **aumentar o prazo** ou **escolher uma opção mais barata**.

                                    ## Acompanhe

                                    Registre cada contribuição na meta. Ver a barra de progresso subir é um ótimo incentivo — e a meta é concluída automaticamente quando você atinge o valor.
                                    """),
                            new LessonSeed("Estratégias para guardar dinheiro", 10, "saving", """
                                    ## 1. Pague-se primeiro

                                    Separe o valor da meta assim que receber.

                                    ## 2. Corte vazamentos

                                    Pequenos gastos repetidos (lanchinhos, microtransações, assinaturas esquecidas) somam muito no mês. Revise-os nos Relatórios.

                                    ## 3. Ganhe um extra

                                    Vender algo que você não usa ou fazer pequenos trabalhos (sempre com autorização dos responsáveis) acelera a meta.

                                    ## 4. Torne visível

                                    Metas com nome e foto mental (\"meu fone\") motivam mais do que \"guardar dinheiro\".

                                    ## 5. Celebre marcos

                                    25%, 50%, 75%... comemore cada etapa sem gastar a meta!

                                    Termine com o quiz **Poupança e metas**.
                                    """))))),
            new CourseSeed("consumo-consciente", "Consumo consciente e segurança",
                    "Compre melhor, resista aos gatilhos e proteja-se de golpes.",
                    "Lojas, apps e jogos são desenhados para estimular compras. Aqui você vai reconhecer os gatilhos de consumo, criar defesas contra o impulso e aprender a identificar golpes financeiros comuns entre jovens.",
                    """
                    Reconhecer gatilhos de urgência e escassez
                    Aplicar técnicas contra compras por impulso
                    Identificar sinais de golpes financeiros
                    Proteger senhas, códigos e dados pessoais
                    """, "CONSCIOUS_CONSUMPTION", "BEGINNER", 30, List.of(
                    new ModuleSeed("Comprar melhor", List.of(
                            new LessonSeed("O que é consumo consciente", 8, null, """
                                    ## Pensar antes de comprar

                                    Consumo consciente é comprar sabendo **por que** você está comprando, **se cabe** no orçamento e **qual o impacto** (para você e para o planeta).

                                    ## Perguntas-chave

                                    1. Eu preciso disso?
                                    2. Posso pagar sem atrapalhar minhas metas?
                                    3. Existe opção usada, emprestada ou mais barata?
                                    4. Vou usar de verdade?

                                    > Consumir com consciência não é deixar de comprar: é comprar o que faz sentido.
                                    """),
                            new LessonSeed("Gatilhos e compras por impulso", 10, null, """
                                    ## Gatilhos comuns

                                    - **Urgência**: \"só hoje!\", contagem regressiva.
                                    - **Escassez**: \"últimas unidades\".
                                    - **Prova social**: \"todo mundo está comprando\".
                                    - **Recompensa aleatória**: caixas de prêmio em jogos (loot boxes).

                                    ## Defesas práticas

                                    - Regra das **24 horas** para compras não planejadas.
                                    - Defina um **limite mensal** para jogos e lazer.
                                    - Desative notificações de lojas e apps de compra.
                                    - Registre toda compra: ver o total no mês muda a percepção.
                                    """))),
                    new ModuleSeed("Segurança", List.of(
                            new LessonSeed("Golpes e segurança digital", 12, "consumption", """
                                    ## Sinais de golpe

                                    - Promessa de **ganho alto, rápido e garantido**.
                                    - Pressão para decidir na hora.
                                    - Pedido de **senha, código SMS** ou dados do cartão.
                                    - Links estranhos e perfis recém-criados.
                                    - \"Oportunidade\" que depende de chamar mais pessoas (pirâmide).

                                    ## Regras de ouro

                                    - **Nunca** compartilhe senhas ou códigos — nem com quem diz ser do banco ou da plataforma.
                                    - Desconfie de prêmios que você não pediu.
                                    - Na dúvida, **pare** e converse com um adulto de confiança.

                                    > Nesta plataforma, nunca pediremos senhas de banco ou dados de cartão.

                                    Finalize com o quiz **Consumo consciente e segurança**.
                                    """))))),
            new CourseSeed("juros-e-inflacao", "Juros e inflação",
                    "Entenda como o dinheiro muda de valor com o tempo.",
                    "Juros e inflação explicam por que R$ 100 hoje não valem o mesmo que R$ 100 daqui a um ano. Você vai calcular juros simples e compostos e entender o efeito da inflação sobre o poder de compra.",
                    """
                    Explicar o que são juros e taxa de juros
                    Calcular juros simples e compostos
                    Explicar o efeito dos juros compostos em investimentos e dívidas
                    Entender a inflação e a perda do poder de compra
                    """, "INTEREST", "INTERMEDIATE", 40, List.of(
                    new ModuleSeed("Juros", List.of(
                            new LessonSeed("O que são juros", 8, null, """
                                    ## O preço do dinheiro no tempo

                                    Juros são a remuneração de quem empresta dinheiro e o custo de quem pega emprestado.

                                    - Quando você **investe**, recebe juros.
                                    - Quando você **deve** (empréstimo, cartão), paga juros.

                                    ## Taxa de juros

                                    É a porcentagem cobrada ou paga **por período**: ao mês (a.m.) ou ao ano (a.a.). Sempre confira o período!

                                    > 2% ao mês é muito diferente de 2% ao ano.
                                    """),
                            new LessonSeed("Juros simples x compostos", 14, "interest", """
                                    ## Juros simples

                                    Os juros incidem sempre sobre o valor inicial:

                                    > Montante = Capital × (1 + taxa × tempo)

                                    R$ 100 a 10% a.m. por 2 meses → 100 × (1 + 0,10 × 2) = **R$ 120**.

                                    ## Juros compostos

                                    Os juros de cada período passam a render também:

                                    > Montante = Capital × (1 + taxa)^tempo

                                    R$ 100 a 10% a.m. por 2 meses → 100 × 1,10 × 1,10 = **R$ 121**.

                                    ## O tempo é o ingrediente principal

                                    No começo a diferença é pequena, mas com muitos períodos os compostos crescem muito mais rápido. Isso é ótimo para quem investe cedo — e perigoso para quem deixa uma dívida crescer.

                                    Experimente na **Calculadora** comparando os dois tipos de juros e depois faça o quiz **Juros simples e compostos**.
                                    """))),
                    new ModuleSeed("Inflação", List.of(
                            new LessonSeed("Inflação e poder de compra", 12, "inflation", """
                                    ## O que é inflação

                                    É o aumento **geral** dos preços ao longo do tempo. No Brasil, o índice oficial é o **IPCA**, calculado pelo IBGE.

                                    ## Poder de compra

                                    Se a inflação foi de 5% em um ano, algo que custava R$ 100 passa a custar cerca de R$ 105. O mesmo dinheiro compra menos.

                                    ## Calculando um aumento

                                    > Aumento % = (preço novo − preço antigo) ÷ preço antigo

                                    Lanche de R$ 20 para R$ 22: (22 − 20) ÷ 20 = **10%**.

                                    ## E o meu dinheiro?

                                    Dinheiro parado, sem render, perde valor real com a inflação. Por isso, ao planejar o longo prazo, é importante entender opções que ao menos acompanhem a inflação — assunto do curso de investimentos.

                                    Faça o quiz **Inflação e poder de compra**.
                                    """))))),
            new CourseSeed("credito-e-dividas", "Crédito e dívidas",
                    "Use o crédito a seu favor e evite a bola de neve das dívidas.",
                    "Cartão de crédito, parcelamento e empréstimos podem ajudar ou atrapalhar. Você vai entender como o crédito funciona, por que o rotativo é tão caro e como prevenir — ou sair — de dívidas.",
                    """
                    Explicar o que é crédito e limite
                    Avaliar compras parceladas pelo custo total
                    Entender o perigo do pagamento mínimo e do rotativo
                    Montar um plano para evitar e sair de dívidas
                    """, "CREDIT", "INTERMEDIATE", 35, List.of(
                    new ModuleSeed("Crédito", List.of(
                            new LessonSeed("Crédito: dinheiro emprestado", 8, null, """
                                    ## O que é crédito

                                    Crédito é dinheiro emprestado que precisa ser devolvido, geralmente **com juros**. Cartão de crédito, cheque especial, empréstimos e financiamentos são formas de crédito.

                                    ## Limite não é renda

                                    O limite do cartão é quanto o banco aceita emprestar. **Não é dinheiro seu.** Tudo o que for gasto aparecerá na fatura.

                                    > Regra de ouro: só use crédito para o que você já teria dinheiro para pagar à vista.
                                    """),
                            new LessonSeed("Cartão de crédito e parcelamento", 12, null, """
                                    ## A fatura

                                    Todo mês chega uma fatura com tudo o que foi comprado. Pagar o **valor total** evita juros.

                                    ## O perigo do mínimo

                                    Pagar só o mínimo joga o restante no **crédito rotativo**, cujos juros estão entre os mais altos do mercado. Uma dívida pequena pode dobrar em poucos meses.

                                    ## Parcelar ou à vista?

                                    - Compare o **preço total** parcelado com o preço à vista.
                                    - Pergunte se há **desconto** à vista.
                                    - Some as parcelas que você já tem: elas cabem no orçamento dos próximos meses?
                                    """))),
                    new ModuleSeed("Dívidas", List.of(
                            new LessonSeed("Endividamento: prevenção e saída", 15, "credit", """
                                    ## Sinais de alerta

                                    - Usar crédito para pagar gastos do dia a dia.
                                    - Não saber quanto deve.
                                    - Pagar só o mínimo da fatura.

                                    ## Prevenção

                                    - Gaste menos do que recebe.
                                    - Tenha reserva de emergência.
                                    - Planeje desejos como metas, em vez de comprar a crédito.

                                    ## Se já existe dívida

                                    1. Liste todas: valor, juros e prazo.
                                    2. Pare de aumentar (corte o uso do crédito).
                                    3. Priorize as de **juros mais altos**.
                                    4. Negocie, com ajuda de um responsável.

                                    > Pedir ajuda cedo é sempre melhor do que esconder o problema.

                                    Termine com o quiz **Crédito e endividamento**.
                                    """))))),
            new CourseSeed("investimentos-iniciantes", "Primeiros passos nos investimentos",
                    "Conceitos essenciais para entender investimentos — sem promessas mágicas.",
                    "Este curso apresenta conceitos introdutórios: risco, retorno, liquidez, renda fixa, renda variável e diversificação. É conteúdo educativo, não recomendação. Pratique na Carteira Simulada, sem dinheiro real.",
                    """
                    Explicar o que é investir e por que começar com metas e reserva
                    Relacionar risco, retorno e liquidez
                    Diferenciar renda fixa e renda variável
                    Explicar a diversificação e o papel do tempo
                    """, "INVESTING", "ADVANCED", 45, List.of(
                    new ModuleSeed("Conceitos básicos", List.of(
                            new LessonSeed("O que é investir", 10, null, """
                                    ## Investir x poupar

                                    Poupar é guardar. Investir é **aplicar** o que foi guardado esperando que renda ao longo do tempo.

                                    ## Antes de investir

                                    - Tenha **metas** claras (para quê e até quando).
                                    - Monte a **reserva de emergência**.
                                    - Entenda o produto antes de aplicar.

                                    ## Menores de idade

                                    Investimentos de menores são feitos em contas abertas e acompanhadas pelos **responsáveis legais**. Converse com sua família.

                                    > Quer praticar? Na Carteira Simulada você testa ideias sem dinheiro real.
                                    """),
                            new LessonSeed("Risco, retorno e liquidez", 12, null, """
                                    ## O triângulo do investimento

                                    - **Retorno**: quanto pode render.
                                    - **Risco**: chance de render menos que o esperado ou perder.
                                    - **Liquidez**: facilidade de resgatar rápido sem perder valor.

                                    Nenhum investimento é ótimo nos três ao mesmo tempo. Em geral, **mais retorno potencial = mais risco**.

                                    ## Desconfie

                                    Promessas de \"retorno alto, garantido e rápido\" não existem no mundo real. São sinal de golpe.
                                    """))),
                    new ModuleSeed("Tipos e estratégias", List.of(
                            new LessonSeed("Renda fixa e renda variável", 12, null, """
                                    ## Renda fixa

                                    As regras de remuneração são conhecidas na aplicação (uma taxa, ou um índice como Selic, CDI ou IPCA). Exemplos: **Tesouro Direto, CDB, poupança**.

                                    ## Renda variável

                                    O retorno não é conhecido antecipadamente e o valor oscila. Exemplos: **ações, fundos imobiliários**.

                                    ## Qual é melhor?

                                    Depende do **objetivo, do prazo e do quanto de oscilação a pessoa suporta**. Por isso não existe \"o melhor investimento\" para todo mundo.

                                    > Este conteúdo é educativo e não é recomendação de investimento.
                                    """),
                            new LessonSeed("Diversificação e o tempo", 11, "investing", """
                                    ## Não coloque todos os ovos na mesma cesta

                                    Diversificar é distribuir o dinheiro entre investimentos diferentes. Se um vai mal, os outros podem compensar. Diversificação **reduz** o risco, mas não o elimina.

                                    ## O tempo a seu favor

                                    Com juros compostos, começar cedo faz grande diferença. Simule na Calculadora: R$ 50 por mês durante 10 anos x durante 20 anos.

                                    ## Lembretes finais

                                    - Simulações usam taxas constantes; a vida real varia.
                                    - Rentabilidade passada não garante rentabilidade futura.
                                    - Decisões reais: sempre com seus responsáveis.

                                    Feche o curso com o quiz **Fundamentos de investimentos**.
                                    """))))));
}

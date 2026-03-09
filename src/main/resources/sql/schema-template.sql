-- Template para criação de tabelas no schema da arena
-- Substitua {SCHEMA_NAME} pelo nome do schema real


-- Tabela: quadras
CREATE TABLE IF NOT EXISTS "{SCHEMA_NAME}".quadras
(
    id_quadra
    SERIAL
    PRIMARY
    KEY,
    nome
    VARCHAR
(
    255
) NOT NULL,
    tipo_quadra VARCHAR
(
    100
),
    valor_hora DECIMAL
(
    10,
    2
),
    ativo BOOLEAN DEFAULT true
    );



CREATE TABLE IF NOT EXISTS "{SCHEMA_NAME}".agendamentos
(
    id_agendamento
    SERIAL
    PRIMARY
    KEY,
    id_quadra
    INT
    NOT
    NULL,
    id_user
    INT
    NOT
    NULL,
    data_inicio
    TIMESTAMP,
    data_fim
    TIMESTAMP,
    status
    VARCHAR
(
    50
),
    valor_total DECIMAL
(
    10,
    2
),
    asaas_payment_id VARCHAR
(
    50
),
    asaas_invoice_url TEXT,
    CONSTRAINT fk_agendamento_quadra FOREIGN KEY
(
    id_quadra
)
    REFERENCES "{SCHEMA_NAME}".quadras
(
    id_quadra
) ON DELETE CASCADE
    );


CREATE INDEX IF NOT EXISTS idx_agendamentos_quadra ON "{SCHEMA_NAME}".agendamentos(id_quadra);
CREATE INDEX IF NOT EXISTS idx_agendamentos_data ON "{SCHEMA_NAME}".agendamentos(data_inicio, data_fim);

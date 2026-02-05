-- Template para criação de tabelas no schema da arena
-- Substitua {SCHEMA_NAME} pelo nome do schema real

-- Tabela: times
CREATE TABLE IF NOT EXISTS "{SCHEMA_NAME}".times (
                                                     id_time SERIAL PRIMARY KEY,
                                                     nome VARCHAR(255),
    id_responsavel INT
    );

-- Tabela: quadras
CREATE TABLE IF NOT EXISTS "{SCHEMA_NAME}".quadras (
                                                       id_quadra SERIAL PRIMARY KEY,
                                                       nome VARCHAR(255) NOT NULL,
    tipo_quadra VARCHAR(100),
    valor_hora DECIMAL(10,2),
    ativo BOOLEAN DEFAULT true
    );

-- Tabela: campeonatos
CREATE TABLE IF NOT EXISTS "{SCHEMA_NAME}".campeonatos (
                                                           id_campeonato SERIAL PRIMARY KEY,
                                                           nome VARCHAR(255) NOT NULL,
    data_inicio TIMESTAMP,
    data_fim TIMESTAMP,
    valor_inscricao DECIMAL(10,2),
    times_min INT,
    premiacao VARCHAR(255),
    status VARCHAR(50)
    );

-- Tabela: inscricoes_campeonato
CREATE TABLE IF NOT EXISTS "{SCHEMA_NAME}".inscricoes_campeonato (
                                                                     id_inscricao SERIAL PRIMARY KEY,
                                                                     id_campeonato INT NOT NULL,
                                                                     id_time INT NOT NULL,
                                                                     data_inscricao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                                     pago BOOLEAN DEFAULT false,
                                                                     CONSTRAINT fk_inscricao_campeonato FOREIGN KEY (id_campeonato)
    REFERENCES "{SCHEMA_NAME}".campeonatos(id_campeonato) ON DELETE CASCADE,
    CONSTRAINT fk_inscricao_time FOREIGN KEY (id_time)
    REFERENCES "{SCHEMA_NAME}".times(id_time) ON DELETE CASCADE
    );


CREATE TABLE IF NOT EXISTS "{SCHEMA_NAME}".agendamentos (
                                                            id_agendamento SERIAL PRIMARY KEY,
                                                            id_quadra INT NOT NULL,
                                                            id_user INT NOT NULL,
                                                            data_inicio TIMESTAMP,
                                                            data_fim TIMESTAMP,
                                                            status VARCHAR(50),
    valor_total DECIMAL(10,2),
    cliente_avulso VARCHAR(255),
    asaas_payment_id VARCHAR(50),
    asaas_invoice_url TEXT,
    pix_qr_code TEXT,
     pix_copy_paste TEXT;
    CONSTRAINT fk_agendamento_quadra FOREIGN KEY (id_quadra)
    REFERENCES "{SCHEMA_NAME}".quadras(id_quadra) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS "{SCHEMA_NAME}".jogos (
    id_jogo SERIAL PRIMARY KEY,
    id_campeonato INT,
    id_time_casa INT,
    id_time_visitante INT,
    placar_visitante SMALLINT,
    placar_casa SMALLINT,
    fase VARCHAR(100),
    status VARCHAR(50),
    id_agendamento INT,
    CONSTRAINT fk_jogo_campeonato FOREIGN KEY (id_campeonato)
    REFERENCES "{SCHEMA_NAME}".campeonatos(id_campeonato) ON DELETE CASCADE,
    CONSTRAINT fk_jogo_time_casa FOREIGN KEY (id_time_casa)
    REFERENCES "{SCHEMA_NAME}".times(id_time) ON DELETE SET NULL,
    CONSTRAINT fk_jogo_time_visitante FOREIGN KEY (id_time_visitante)
    REFERENCES "{SCHEMA_NAME}".times(id_time) ON DELETE SET NULL,
    CONSTRAINT fk_jogo_agendamento FOREIGN KEY (id_agendamento)
    REFERENCES "{SCHEMA_NAME}".agendamentos(id_agendamento) ON DELETE SET NULL
    );

-- Índices para melhorar performance
CREATE INDEX IF NOT EXISTS idx_agendamentos_quadra ON "{SCHEMA_NAME}".agendamentos(id_quadra);
CREATE INDEX IF NOT EXISTS idx_agendamentos_data ON "{SCHEMA_NAME}".agendamentos(data_inicio, data_fim);
CREATE INDEX IF NOT EXISTS idx_jogos_campeonato ON "{SCHEMA_NAME}".jogos(id_campeonato);

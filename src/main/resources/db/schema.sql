-- Executado na inicialização. Não remove ou substitui dados existentes.
CREATE TABLE IF NOT EXISTS agendamento (
    id VARCHAR(36) PRIMARY KEY,
    cliente VARCHAR(80) NOT NULL,
    telefone VARCHAR(11) NOT NULL,
    servico VARCHAR(20) NOT NULL CHECK (servico IN ('CORTE', 'BARBA', 'COMBO')),
    barbeiro VARCHAR(20) NOT NULL CHECK (barbeiro IN ('ANDRE', 'BRUNO', 'CAIO')),
    inicio TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_agendamento_barbeiro_inicio ON agendamento (barbeiro, inicio);

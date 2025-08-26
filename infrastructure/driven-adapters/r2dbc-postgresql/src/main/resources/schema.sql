-- Schema para microservicio de solicitudes
-- Base de datos: PostgreSQL

-- Tabla de tipos de préstamo
CREATE TABLE IF NOT EXISTS loan_types (
    loan_type_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    interest_rate DECIMAL(5,4) NOT NULL,
    minimum_amount DECIMAL(15,2) NOT NULL DEFAULT 100000.00,
    max_amount DECIMAL(15,2) NOT NULL,
    automatic_validation BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de estados
CREATE TABLE IF NOT EXISTS states (
    state_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de solicitudes
CREATE TABLE IF NOT EXISTS applications (
    application_id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(15) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    term INTEGER NOT NULL,
    email VARCHAR(255) NOT NULL,
    state_id BIGINT NOT NULL,
    loan_type_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_application_state 
        FOREIGN KEY (state_id) REFERENCES states(state_id),
    CONSTRAINT fk_application_loan_type 
        FOREIGN KEY (loan_type_id) REFERENCES loan_types(loan_type_id),
    CONSTRAINT chk_amount_positive 
        CHECK (amount > 0),
    CONSTRAINT chk_term_positive 
        CHECK (term > 0)
);

-- Índices para optimizar consultas
CREATE INDEX IF NOT EXISTS idx_applications_email ON applications(email);
CREATE INDEX IF NOT EXISTS idx_applications_state ON applications(state_id);
CREATE INDEX IF NOT EXISTS idx_applications_loan_type ON applications(loan_type_id);

-- Datos iniciales para estados
INSERT INTO states (name, description) VALUES 
    ('Pendiente de revision', 'Solicitud recibida, pendiente de evaluacion inicial'),
    ('En evaluacion', 'Solicitud en proceso de analisis crediticio'),
    ('Aprobada', 'Solicitud aprobada, pendiente de desembolso'),
    ('Rechazada', 'Solicitud rechazada por no cumplir criterios'),
    ('Desembolsada', 'Prestamo desembolsado exitosamente'),
    ('Cancelada', 'Solicitud cancelada por el solicitante')
ON CONFLICT (name) DO NOTHING;

-- Datos iniciales para tipos de préstamo
INSERT INTO loan_types (name, interest_rate, minimum_amount, max_amount, automatic_validation) VALUES 
    ('Prestamo Personal', 0.1250, 100000.00, 50000000.00, true),
    ('Prestamo Hipotecario', 0.0890, 10000000.00, 500000000.00, false),
    ('Prestamo Vehicular', 0.1150, 5000000.00, 100000000.00, true),
    ('Microcredito', 0.1850, 100000.00, 5000000.00, true),
    ('Prestamo Empresarial', 0.1050, 1000000.00, 1000000000.00, false)
ON CONFLICT (name) DO NOTHING;

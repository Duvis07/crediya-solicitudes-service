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
    ('Revision manual', 'Solicitud requiere revision manual por un asesor'),
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

-- Datos de prueba para testing HU4
-- Insertar solicitudes con diferentes estados para probar filtros

-- Solicitud 1: Pendiente de revision
INSERT INTO applications (document_id, amount, term, email, state_id, loan_type_id) 
VALUES ('12345678', 5000000.00, 24, 'juan.perez@example.com', 
        (SELECT state_id FROM states WHERE name = 'Pendiente de revision'),
        (SELECT loan_type_id FROM loan_types WHERE name = 'Prestamo Personal'));

-- Solicitud 2: Rechazada
INSERT INTO applications (document_id, amount, term, email, state_id, loan_type_id) 
VALUES ('87654321', 15000000.00, 36, 'maria.garcia@example.com', 
        (SELECT state_id FROM states WHERE name = 'Rechazada'),
        (SELECT loan_type_id FROM loan_types WHERE name = 'Prestamo Vehicular'));

-- Solicitud 3: Revision manual
INSERT INTO applications (document_id, amount, term, email, state_id, loan_type_id) 
VALUES ('11223344', 25000000.00, 48, 'carlos.lopez@example.com', 
        (SELECT state_id FROM states WHERE name = 'Revision manual'),
        (SELECT loan_type_id FROM loan_types WHERE name = 'Prestamo Hipotecario'));

-- Solicitud 4: Aprobada (NO debe aparecer en el filtro)
INSERT INTO applications (document_id, amount, term, email, state_id, loan_type_id) 
VALUES ('55667788', 3000000.00, 12, 'ana.martinez@example.com', 
        (SELECT state_id FROM states WHERE name = 'Aprobada'),
        (SELECT loan_type_id FROM loan_types WHERE name = 'Microcredito'));

-- Solicitud 5: Otra Pendiente de revision
INSERT INTO applications (document_id, amount, term, email, state_id, loan_type_id) 
VALUES ('99887766', 8000000.00, 30, 'pedro.sanchez@example.com', 
        (SELECT state_id FROM states WHERE name = 'Pendiente de revision'),
        (SELECT loan_type_id FROM loan_types WHERE name = 'Prestamo Personal'));

-- Solicitudes DESEMBOLSADAS para testing de deuda total mensual
-- Juan Pérez: 2 préstamos desembolsados
INSERT INTO applications (document_id, amount, term, email, state_id, loan_type_id) 
VALUES ('12345678', 10000000.00, 36, 'juan.perez@example.com', 
        (SELECT state_id FROM states WHERE name = 'Desembolsada'),
        (SELECT loan_type_id FROM loan_types WHERE name = 'Prestamo Personal'));

INSERT INTO applications (document_id, amount, term, email, state_id, loan_type_id) 
VALUES ('12345678', 8000000.00, 24, 'juan.perez@example.com', 
        (SELECT state_id FROM states WHERE name = 'Desembolsada'),
        (SELECT loan_type_id FROM loan_types WHERE name = 'Prestamo Vehicular'));

-- María García: 1 préstamo desembolsado
INSERT INTO applications (document_id, amount, term, email, state_id, loan_type_id) 
VALUES ('87654321', 15000000.00, 48, 'maria.garcia@example.com', 
        (SELECT state_id FROM states WHERE name = 'Desembolsada'),
        (SELECT loan_type_id FROM loan_types WHERE name = 'Prestamo Hipotecario'));

-- Carlos López: 1 préstamo desembolsado
INSERT INTO applications (document_id, amount, term, email, state_id, loan_type_id) 
VALUES ('11223344', 20000000.00, 60, 'carlos.lopez@example.com', 
        (SELECT state_id FROM states WHERE name = 'Desembolsada'),
        (SELECT loan_type_id FROM loan_types WHERE name = 'Prestamo Personal'));

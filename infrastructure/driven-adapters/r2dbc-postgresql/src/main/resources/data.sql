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

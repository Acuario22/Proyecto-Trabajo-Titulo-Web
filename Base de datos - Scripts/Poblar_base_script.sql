USE BD_REPORTES_URBANOS;

-- ---------------------------------------------------------------------
-- 1. REPORTES DE PRUEBA
-- ---------------------------------------------------------------------

-- Reporte 1: Pavimentación - Escalado por regla (Media -> Alta)
INSERT INTO TBLREPORTES (
    FECHA_INGRESO, TEXTO_REPORTE, DIRECCION,
    ID_CATEGORIA, ID_SENTIMIENTO, ID_URGENCIA_ML,
    NIVEL_RIESGO, ID_URGENCIA_FINAL, HUBO_AJUSTE, ID_ESTADO
) VALUES (
    '2026-10-15 08:30:00',
    'Existe un bache profundo en Avenida Libertad con Calle 4. Varios vehículos han reventado neumáticos y está cerca de una curva ciega.',
    'Av. Libertad 1240, Sector Centro',
    1, -- PAVIMENTACION
    1, -- ENOJADO
    2, -- MEDIA (ML)
    'ALTO',
    3, -- ALTA (Escalado por riesgo)
    TRUE,
    1  -- NO INICIADO
);

-- Reporte 2: Alcantarillado - Escalado crítico (Media -> Alta)
INSERT INTO TBLREPORTES (
    FECHA_INGRESO, TEXTO_REPORTE, DIRECCION,
    ID_CATEGORIA, ID_SENTIMIENTO, ID_URGENCIA_ML,
    NIVEL_RIESGO, ID_URGENCIA_FINAL, HUBO_AJUSTE, ID_ESTADO
) VALUES (
    '2026-10-16 11:15:00',
    'Tapa de cámara de alcantarillado rota y abierta frente a la escuela básica. Emana agua servida y hay riesgo inminente de caída peatonal.',
    'Calle Los Aromos frente al N° 450',
    2, -- ALCANTARILLADO
    3, -- FRUSTRADO
    2, -- MEDIA (ML)
    'CRITICO',
    3, -- ALTA (Escalado por riesgo crítico)
    TRUE,
    2  -- INICIADO
);

-- Reporte 3: Iluminación - Sin alteración de regla (Baja se mantiene)
INSERT INTO TBLREPORTES (
    FECHA_INGRESO, TEXTO_REPORTE, DIRECCION,
    ID_CATEGORIA, ID_SENTIMIENTO, ID_URGENCIA_ML,
    NIVEL_RIESGO, ID_URGENCIA_FINAL, HUBO_AJUSTE, ID_ESTADO
) VALUES (
    '2026-10-17 19:40:00',
    'Una de las luminarias de la plaza vecinal parpadea por momentos. La visibilidad general sigue siendo adecuada.',
    'Plaza Las Lilas, Pasaje Oriente',
    3, -- ILUMINACION
    2, -- NEUTRAL
    1, -- BAJA (ML)
    'NINGUNO',
    1, -- BAJA (Final igual a ML)
    FALSE,
    1  -- NO INICIADO
);

-- ---------------------------------------------------------------------
-- 2. ORDEN DE TRABAJO (asociada al Reporte 2 en estado INICIADO)
-- ---------------------------------------------------------------------
INSERT INTO TBLORDENTRABAJO (
    ID_REPORTE, CUADRILLA, RESPONSABLE, CANTIDAD_PERSONAL, VEHICULO,
    FECHA_VISITA, HORA_VISITA, DURACION_ESTIMADA, MATERIALES, INSTRUCCIONES
) VALUES (
    2,
    'Cuadrilla de Emergencias Sanitarias',
    'Carlos Méndez',
    4,
    'Camioneta Técnica OP-07',
    '2026-10-17',
    '09:00:00',
    '3 horas',
    'Tapa de cámara de hierro fundido, señalética reflectante y vallas de seguridad.',
    'Asegurar el perímetro escolar, retirar restos de la tapa dañada e instalar reposición definitiva.'
);

-- ---------------------------------------------------------------------
-- 3. HISTORIAL INICIAL (TRAZABILIDAD)
-- ---------------------------------------------------------------------
INSERT INTO TBLHISTORIAL (ID_REPORTE, ID_ESTADO, FECHA_CAMBIO, OBSERVACION) VALUES
    (1, 1, '2026-10-15 08:30:00', 'Reporte ingresado y clasificado automáticamente por el sistema.'),
    (2, 1, '2026-10-16 11:15:00', 'Reporte ingresado y clasificado automáticamente con alerta crítica.'),
    (2, 2, '2026-10-16 11:45:00', 'Orden de trabajo emitida y asignada a Cuadrilla de Emergencias Sanitarias.'),
    (3, 1, '2026-10-17 19:40:00', 'Reporte ingresado y clasificado automáticamente por el sistema.');
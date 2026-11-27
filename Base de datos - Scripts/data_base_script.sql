-- =====================================================================
-- Script de creación e inicialización de la base de datos del sistema
-- de gestión de reportes ciudadanos de infraestructura urbana
-- =====================================================================
CREATE DATABASE IF NOT EXISTS BD_REPORTES_URBANOS
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE BD_REPORTES_URBANOS;

-- ---------------------------------------------------------------------
-- 1. TABLAS DE CATÁLOGO
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS TBLCATEGORIA (
    ID_CATEGORIA   INT           NOT NULL AUTO_INCREMENT,
    NOMBRE         VARCHAR(50)   NOT NULL,
    DESCRIPCION    VARCHAR(500)  NULL,
    PRIMARY KEY (ID_CATEGORIA),
    UNIQUE KEY UQ_CATEGORIA_NOMBRE (NOMBRE)
) ENGINE = InnoDB;

-- NIVEL permite comparar y escalar urgencias numéricamente
-- (el motor de reglas sube la urgencia como NIVEL + 1, hasta el máximo).
CREATE TABLE IF NOT EXISTS TBLURGENCIA (
    ID_URGENCIA    INT           NOT NULL AUTO_INCREMENT,
    NOMBRE         VARCHAR(20)   NOT NULL,
    NIVEL          INT           NOT NULL,
    PRIMARY KEY (ID_URGENCIA),
    UNIQUE KEY UQ_URGENCIA_NOMBRE (NOMBRE),
    UNIQUE KEY UQ_URGENCIA_NIVEL (NIVEL)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS TBLSENTIMIENTO (
    ID_SENTIMIENTO INT           NOT NULL AUTO_INCREMENT,
    NOMBRE         VARCHAR(20)   NOT NULL,
    PRIMARY KEY (ID_SENTIMIENTO),
    UNIQUE KEY UQ_SENTIMIENTO_NOMBRE (NOMBRE)
) ENGINE = InnoDB;

-- ORDEN define la secuencia de la línea de tiempo de trazabilidad (1 al 4).
CREATE TABLE IF NOT EXISTS TBLESTADOREPORTE (
    ID_ESTADO      INT           NOT NULL AUTO_INCREMENT,
    NOMBRE         VARCHAR(30)   NOT NULL,
    ORDEN          INT           NOT NULL,
    PRIMARY KEY (ID_ESTADO),
    UNIQUE KEY UQ_ESTADO_NOMBRE (NOMBRE),
    UNIQUE KEY UQ_ESTADO_ORDEN (ORDEN)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 2. TABLA PRINCIPAL DE REPORTES
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS TBLREPORTES (
    ID_REPORTE          INT           NOT NULL AUTO_INCREMENT,
    FECHA_INGRESO       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    TEXTO_REPORTE       TEXT          NOT NULL,
    DIRECCION           VARCHAR(255)  NULL,

    -- Salida de los modelos de Machine Learning
    ID_CATEGORIA        INT           NOT NULL,
    ID_SENTIMIENTO      INT           NOT NULL,
    ID_URGENCIA_ML      INT           NOT NULL,

    -- Salida del motor de priorización basado en reglas
    NIVEL_RIESGO        ENUM('ALTO', 'CRITICO', 'NINGUNO') NOT NULL DEFAULT 'NINGUNO',
    ID_URGENCIA_FINAL   INT           NOT NULL,
    HUBO_AJUSTE         BOOLEAN       NOT NULL DEFAULT FALSE,

    -- Gestión: estado actual del reporte (por defecto, 1 = NO INICIADO)
    ID_ESTADO           INT           NOT NULL DEFAULT 1,

    PRIMARY KEY (ID_REPORTE),

    CONSTRAINT FK_REPORTES_CATEGORIA
        FOREIGN KEY (ID_CATEGORIA)      REFERENCES TBLCATEGORIA (ID_CATEGORIA),
    CONSTRAINT FK_REPORTES_SENTIMIENTO
        FOREIGN KEY (ID_SENTIMIENTO)    REFERENCES TBLSENTIMIENTO (ID_SENTIMIENTO),
    CONSTRAINT FK_REPORTES_URGENCIA_ML
        FOREIGN KEY (ID_URGENCIA_ML)    REFERENCES TBLURGENCIA (ID_URGENCIA),
    CONSTRAINT FK_REPORTES_URGENCIA_FINAL
        FOREIGN KEY (ID_URGENCIA_FINAL) REFERENCES TBLURGENCIA (ID_URGENCIA),
    CONSTRAINT FK_REPORTES_ESTADO
        FOREIGN KEY (ID_ESTADO)         REFERENCES TBLESTADOREPORTE (ID_ESTADO)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 3. TABLAS DE GESTIÓN OPERATIVA Y TRAZABILIDAD
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS TBLORDENTRABAJO (
    ID_ORDEN                 INT           NOT NULL AUTO_INCREMENT,
    ID_REPORTE               INT           NOT NULL,
    CUADRILLA                VARCHAR(100)  NULL,
    RESPONSABLE              VARCHAR(100)  NULL,
    CANTIDAD_PERSONAL        SMALLINT      NULL,
    VEHICULO                 VARCHAR(100)  NULL,
    FECHA_VISITA             DATE          NULL,
    HORA_VISITA              TIME          NULL,
    DURACION_ESTIMADA        VARCHAR(50)   NULL,
    MATERIALES               VARCHAR(500)  NULL,
    INSTRUCCIONES            VARCHAR(1000) NULL,
    FECHA_CIERRE_ADMIN       DATETIME      NULL,
    OBSERVACION_CIERRE_ADMIN VARCHAR(500)  NULL,

    PRIMARY KEY (ID_ORDEN),
    UNIQUE KEY UQ_ORDENTRABAJO_REPORTE (ID_REPORTE),

    CONSTRAINT FK_ORDENTRABAJO_REPORTE
        FOREIGN KEY (ID_REPORTE) REFERENCES TBLREPORTES (ID_REPORTE)
        ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS TBLHISTORIAL (
    ID_HISTORIAL   INT           NOT NULL AUTO_INCREMENT,
    ID_REPORTE     INT           NOT NULL,
    ID_ESTADO      INT           NOT NULL,
    FECHA_CAMBIO   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    OBSERVACION    VARCHAR(500)  NULL,

    PRIMARY KEY (ID_HISTORIAL),

    CONSTRAINT FK_HISTORIAL_REPORTE
        FOREIGN KEY (ID_REPORTE) REFERENCES TBLREPORTES (ID_REPORTE)
        ON DELETE CASCADE,
    CONSTRAINT FK_HISTORIAL_ESTADO
        FOREIGN KEY (ID_ESTADO)  REFERENCES TBLESTADOREPORTE (ID_ESTADO)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 4. DATOS INICIALES DE LOS CATÁLOGOS
-- ---------------------------------------------------------------------
INSERT IGNORE INTO TBLCATEGORIA (NOMBRE, DESCRIPCION) VALUES
    ('PAVIMENTACION',  'Fallas estructurales en la superficie de rodado vial y peatonal: baches, grietas, hundimientos de aceras y reductores de velocidad en mal estado.'),
    ('ALCANTARILLADO', 'Anomalías en la red secundaria de distribución hídrica y recolección de aguas servidas visibles en la vía pública: tuberías rotas, filtraciones, inundaciones y tapas faltantes o dañadas.'),
    ('ILUMINACION',    'Deficiencias en el alumbrado público que comprometen la visibilidad nocturna: luminarias apagadas, focos quemados, postes dañados o ausencia de alumbrado.');

INSERT IGNORE INTO TBLURGENCIA (NOMBRE, NIVEL) VALUES
    ('BAJA',  0),
    ('MEDIA', 1),
    ('ALTA',  2);

INSERT IGNORE INTO TBLSENTIMIENTO (NOMBRE) VALUES
    ('ENOJADO'),
    ('NEUTRAL'),
    ('FRUSTRADO');

INSERT IGNORE INTO TBLESTADOREPORTE (ID_ESTADO, NOMBRE, ORDEN) VALUES
    (1, 'NO INICIADO', 1),
    (2, 'INICIADO',    2),
    (3, 'EN REVISION', 3),
    (4, 'FINALIZADO',  4);
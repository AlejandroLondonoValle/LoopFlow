-- ============================================================
-- LoopFlow — Script de Base de Datos MySQL 8.0+
-- Autor: LoopFlow Dev Team
-- Descripción: Crea la base de datos y todas las tablas del
--   Sistema de Gestión de Hábitos y Productividad Personal.
-- Instrucciones: Ejecutar en MySQL Workbench o cliente MySQL:
--   mysql -u <usuario> -p < loopflow_schema.sql
-- ============================================================

-- Crear base de datos con charset UTF-8 completo (emojis incluidos)
CREATE DATABASE IF NOT EXISTS loopflow_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE loopflow_db;

-- ============================================================
-- TABLE: categories
-- Categorías personalizables para agrupar hábitos.
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL                        COMMENT 'Nombre de la categoría',
    color       VARCHAR(7)      NOT NULL DEFAULT '#2F49A1'      COMMENT 'Color hexadecimal (#RRGGBB)',
    icon        VARCHAR(50)     NULL                            COMMENT 'Nombre del icono (ej: heart, zap, book)',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_categories   PRIMARY KEY (id),
    CONSTRAINT uq_category_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Categorías de hábitos';

-- ============================================================
-- TABLE: habits
-- Hábitos del usuario con frecuencia y meta en días.
-- ============================================================
CREATE TABLE IF NOT EXISTS habits (
    id          INT UNSIGNED        NOT NULL AUTO_INCREMENT,
    category_id INT UNSIGNED        NOT NULL,
    name        VARCHAR(200)        NOT NULL                    COMMENT 'Nombre del hábito',
    description TEXT                NULL                        COMMENT 'Descripción detallada',
    frequency   ENUM('DAILY','WEEKLY','MONTHLY')
                                    NOT NULL DEFAULT 'DAILY'    COMMENT 'Frecuencia de ejecución',
    goal_days   INT UNSIGNED        NOT NULL DEFAULT 30         COMMENT 'Meta en días a alcanzar',
    start_date  DATE                NOT NULL                    COMMENT 'Fecha de inicio del hábito',
    is_active   TINYINT(1)          NOT NULL DEFAULT 1          COMMENT '1=activo, 0=archivado',
    created_at  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_habits            PRIMARY KEY (id),
    CONSTRAINT fk_habits_category   FOREIGN KEY (category_id)
        REFERENCES categories(id) ON DELETE CASCADE ON UPDATE CASCADE,

    INDEX idx_habits_category  (category_id),
    INDEX idx_habits_active    (is_active),
    INDEX idx_habits_frequency (frequency),
    INDEX idx_habits_start     (start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Hábitos del usuario';

-- ============================================================
-- TABLE: daily_logs
-- Registro diario de cumplimiento de cada hábito.
-- Restricción UNIQUE (habit_id, log_date) garantiza un solo
-- registro por hábito por día → UPSERT seguro.
-- ============================================================
CREATE TABLE IF NOT EXISTS daily_logs (
    id          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    habit_id    INT UNSIGNED    NOT NULL,
    log_date    DATE            NOT NULL                        COMMENT 'Fecha del registro',
    completed   TINYINT(1)      NOT NULL DEFAULT 0             COMMENT '1=completado, 0=pendiente',
    notes       TEXT            NULL                            COMMENT 'Notas opcionales del día',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_daily_logs            PRIMARY KEY (id),
    CONSTRAINT fk_daily_logs_habit      FOREIGN KEY (habit_id)
        REFERENCES habits(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT uq_daily_logs_habit_date UNIQUE (habit_id, log_date),

    INDEX idx_daily_logs_habit   (habit_id),
    INDEX idx_daily_logs_date    (log_date),
    INDEX idx_daily_logs_completed (completed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Registros diarios de hábitos';

-- ============================================================
-- TABLE: tasks
-- Tareas del tablero Scrum/Kanban con estado y prioridad.
-- ============================================================
CREATE TABLE IF NOT EXISTS tasks (
    id          INT UNSIGNED        NOT NULL AUTO_INCREMENT,
    title       VARCHAR(300)        NOT NULL                    COMMENT 'Título de la tarea',
    description TEXT                NULL                        COMMENT 'Descripción detallada',
    status      ENUM('TODO','IN_PROGRESS','DONE')
                                    NOT NULL DEFAULT 'TODO'     COMMENT 'Estado en el tablero Kanban',
    priority    ENUM('LOW','MEDIUM','HIGH','CRITICAL')
                                    NOT NULL DEFAULT 'MEDIUM'   COMMENT 'Nivel de prioridad',
    due_date    DATE                NULL                        COMMENT 'Fecha límite (opcional)',
    created_at  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_tasks PRIMARY KEY (id),

    INDEX idx_tasks_status   (status),
    INDEX idx_tasks_priority (priority),
    INDEX idx_tasks_due_date (due_date),
    INDEX idx_tasks_created  (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tareas del tablero Scrum Kanban';

-- ============================================================
-- TABLE: task_history
-- Historial de cambios de estado de cada tarea.
-- Permite auditar el movimiento de tareas entre columnas Kanban.
-- ============================================================
CREATE TABLE IF NOT EXISTS task_history (
    id          INT UNSIGNED        NOT NULL AUTO_INCREMENT,
    task_id     INT UNSIGNED        NOT NULL,
    old_status  ENUM('TODO','IN_PROGRESS','DONE')
                                    NULL                        COMMENT 'Estado anterior (NULL = creación)',
    new_status  ENUM('TODO','IN_PROGRESS','DONE')
                                    NOT NULL                    COMMENT 'Estado nuevo',
    changed_at  DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes       TEXT                NULL                        COMMENT 'Notas del cambio',

    CONSTRAINT pk_task_history          PRIMARY KEY (id),
    CONSTRAINT fk_task_history_task     FOREIGN KEY (task_id)
        REFERENCES tasks(id) ON DELETE CASCADE ON UPDATE CASCADE,

    INDEX idx_task_history_task       (task_id),
    INDEX idx_task_history_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Historial de cambios de estado de tareas';

-- ============================================================
-- TABLE: system_config
-- Configuraciones globales de la aplicación (clave-valor).
-- ============================================================
CREATE TABLE IF NOT EXISTS system_config (
    id          INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    config_key  VARCHAR(100)    NOT NULL                        COMMENT 'Clave de configuración',
    config_value TEXT           NOT NULL                        COMMENT 'Valor de la configuración',
    description VARCHAR(500)    NULL                            COMMENT 'Descripción del parámetro',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_system_config     PRIMARY KEY (id),
    CONSTRAINT uq_system_config_key UNIQUE (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Configuraciones del sistema LoopFlow';

-- ============================================================
-- DATOS INICIALES
-- ============================================================

-- Categorías por defecto
INSERT INTO categories (name, color, icon) VALUES
    ('Salud',         '#22C55E', 'heart'),
    ('Productividad', '#2F49A1', 'zap'),
    ('Aprendizaje',   '#F59E0B', 'book-open'),
    ('Ejercicio',     '#EF4444', 'activity'),
    ('Bienestar',     '#8B5CF6', 'star')
ON DUPLICATE KEY UPDATE color = VALUES(color);

-- Configuración del sistema por defecto
INSERT INTO system_config (config_key, config_value, description) VALUES
    ('theme',               'light',            'Tema visual: light | dark'),
    ('language',            'es',               'Idioma de la interfaz'),
    ('timezone',            'America/Bogota',   'Zona horaria del usuario'),
    ('habit_reminder_time', '08:00',            'Hora del recordatorio diario de hábitos (HH:mm)'),
    ('week_starts_on',      'MONDAY',           'Día de inicio de semana: MONDAY | SUNDAY'),
    ('dashboard_layout',    'unified',          'Disposición del dashboard: unified | split')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

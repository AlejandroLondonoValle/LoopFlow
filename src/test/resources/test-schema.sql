-- ============================================================
-- LoopFlow — Esquema H2 para Tests Unitarios
-- Modo: H2 con compatibilidad MySQL
-- Nota: H2 no soporta ENUM ni FIELD() — se usan VARCHAR y ORDER BY estándar
-- ============================================================

CREATE TABLE IF NOT EXISTS categories (
    id          INT             NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL,
    color       VARCHAR(7)      NOT NULL DEFAULT '#2F49A1',
    icon        VARCHAR(50)     NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_category_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS habits (
    id          INT             NOT NULL AUTO_INCREMENT,
    category_id INT             NOT NULL,
    name        VARCHAR(200)    NOT NULL,
    description TEXT            NULL,
    frequency   VARCHAR(20)     NOT NULL DEFAULT 'DAILY',
    goal_days   INT             NOT NULL DEFAULT 30,
    start_date  DATE            NOT NULL,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS daily_logs (
    id          INT             NOT NULL AUTO_INCREMENT,
    habit_id    INT             NOT NULL,
    log_date    DATE            NOT NULL,
    completed   BOOLEAN         NOT NULL DEFAULT FALSE,
    notes       TEXT            NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE,
    CONSTRAINT uq_daily_logs_habit_date UNIQUE (habit_id, log_date)
);

CREATE TABLE IF NOT EXISTS tasks (
    id          INT             NOT NULL AUTO_INCREMENT,
    title       VARCHAR(300)    NOT NULL,
    description TEXT            NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'TODO',
    priority    VARCHAR(20)     NOT NULL DEFAULT 'MEDIUM',
    due_date    DATE            NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS task_history (
    id          INT             NOT NULL AUTO_INCREMENT,
    task_id     INT             NOT NULL,
    old_status  VARCHAR(20)     NULL,
    new_status  VARCHAR(20)     NOT NULL,
    changed_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes       TEXT            NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS system_config (
    id          INT             NOT NULL AUTO_INCREMENT,
    config_key  VARCHAR(100)    NOT NULL,
    config_value TEXT           NOT NULL,
    description VARCHAR(500)    NULL,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_system_config_key UNIQUE (config_key)
);

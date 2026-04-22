CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_email  VARCHAR(255) NOT NULL,
    type        VARCHAR(32) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    message     VARCHAR(1024) NOT NULL,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL
);

CREATE INDEX idx_notifications_user_email ON notifications (user_email);
CREATE INDEX idx_notifications_user_unread ON notifications (user_email, is_read);

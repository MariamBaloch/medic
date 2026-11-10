DROP INDEX IF EXISTS idx_outbox_unpublished;

CREATE INDEX idx_outbox_unpublished
    ON outbox_event (id)
    WHERE published_at IS NULL;

ALTER TABLE gifts
    ADD COLUMN event_id BIGINT;

UPDATE gifts gift
SET event_id = event.id
FROM events event
WHERE event.type = 'WEDDING';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM gifts WHERE event_id IS NULL) THEN
        RAISE EXCEPTION
            'Não foi possível migrar gifts: o evento WEDDING não foi encontrado.';
    END IF;
END
$$;

ALTER TABLE gifts
    ALTER COLUMN event_id SET NOT NULL,
    ADD CONSTRAINT fk_gifts_event
        FOREIGN KEY (event_id) REFERENCES events (id);

CREATE INDEX idx_gifts_event_id
    ON gifts (event_id, id);

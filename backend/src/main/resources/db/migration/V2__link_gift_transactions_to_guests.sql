ALTER TABLE gift_transactions
    ADD COLUMN IF NOT EXISTS guest_id BIGINT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'gift_transactions'
          AND column_name = 'guest_name'
    ) THEN
        IF EXISTS (
            SELECT 1
            FROM guests guest
            JOIN gift_transactions gift_tx
              ON lower(trim(gift_tx.guest_name)) = lower(trim(guest.full_name))
            GROUP BY lower(trim(guest.full_name))
            HAVING count(DISTINCT guest.id) > 1
        ) THEN
            RAISE EXCEPTION
                'Não foi possível migrar gift_transactions: existem convidados homônimos associados a reservas.';
        END IF;

        UPDATE gift_transactions gift_tx
        SET guest_id = guest.id
        FROM guests guest
        WHERE gift_tx.guest_id IS NULL
          AND lower(trim(gift_tx.guest_name)) = lower(trim(guest.full_name));
    END IF;

    IF EXISTS (SELECT 1 FROM gift_transactions WHERE guest_id IS NULL) THEN
        RAISE EXCEPTION
            'Não foi possível migrar gift_transactions: há reservas sem convidado correspondente.';
    END IF;
END
$$;

ALTER TABLE gift_transactions
    ALTER COLUMN guest_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_gift_transactions_guest'
    ) THEN
        ALTER TABLE gift_transactions
            ADD CONSTRAINT fk_gift_transactions_guest
            FOREIGN KEY (guest_id) REFERENCES guests (id);
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_gift_transactions_guest_id
    ON gift_transactions (guest_id);

DROP INDEX IF EXISTS idx_gift_transactions_guest_name;

ALTER TABLE gift_transactions
    DROP COLUMN IF EXISTS guest_name;

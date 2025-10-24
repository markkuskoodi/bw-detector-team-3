CREATE INDEX IF NOT EXISTS transaction_sender_id_idx on transaction(sender_id);
CREATE INDEX IF NOT EXISTS transaction_timestamp_idx on transaction(timestamp);
CREATE INDEX IF NOT EXISTS person_person_code_idx on person(person_code);
CREATE INDEX IF NOT EXISTS device_mac_idx on device(mac);
CREATE INDEX IF NOT EXISTS account_number_idx on account(number);

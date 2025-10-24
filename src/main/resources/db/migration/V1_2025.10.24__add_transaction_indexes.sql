-- Index for sender-based queries (most common query pattern)
CREATE INDEX idx_transaction_sender_id ON transaction(sender_id);

-- Index for timestamp filtering (used in time-based validations)
CREATE INDEX idx_transaction_timestamp ON transaction(timestamp);

-- Composite index for the common query pattern: sender + timestamp
CREATE INDEX idx_transaction_sender_timestamp ON transaction(sender_id, timestamp);

-- Index for device queries
CREATE INDEX idx_transaction_device_id ON transaction(device_id);

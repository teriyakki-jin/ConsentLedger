-- Change payload_json from jsonb to text to preserve exact bytes for hash chain integrity
ALTER TABLE audit_logs ALTER COLUMN payload_json TYPE text;

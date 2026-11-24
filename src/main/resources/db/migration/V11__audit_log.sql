CREATE TABLE IF NOT EXISTS audit_log (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  created_at timestamp NOT NULL,
  actor_username TEXT,
  action TEXT NOT NULL,
  entity_type TEXT NOT NULL,
  entity_id TEXT,
  details TEXT
);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at);

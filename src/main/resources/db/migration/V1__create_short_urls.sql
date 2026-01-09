CREATE TABLE short_urls (
  id BIGSERIAL PRIMARY KEY,
  original_url TEXT NOT NULL UNIQUE,
  code VARCHAR(10) UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX ux_short_urls_code ON short_urls(code);
CREATE UNIQUE INDEX ux_short_urls_original_url ON short_urls(original_url);
CREATE INDEX ix_short_urls_expires_at ON short_urls(expires_at);

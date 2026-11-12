CREATE TABLE IF NOT EXISTS ingestion_jobs (
    job_id uuid PRIMARY KEY,
    notebook_id uuid NOT NULL REFERENCES notebooks(notebook_id) ON DELETE CASCADE,
    job_type varchar(20) NOT NULL,
    status varchar(20) NOT NULL,
    document_title varchar(500),
    file_name varchar(500),
    source_urls text NOT NULL DEFAULT '',
    error_code varchar(80),
    error_message text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_notebook_created
    ON ingestion_jobs(notebook_id, created_at DESC);

-- =============================================================================
-- Supabase SQL Migration: 001_notifications_schema.sql
-- AI-Based End-to-End Recruitment System
-- Run this in your Supabase SQL Editor (Dashboard -> SQL Editor -> New Query)
-- =============================================================================

-- ---------------------------------------------------------------------------
-- NOTIFICATIONS TABLE
-- Stores real-time notifications. recipient_id maps to the MongoDB
-- candidate/recruiter ID (e.g. c-0001, hr-0001).
-- The backend writes using service_role key (bypasses RLS).
-- The frontend reads using anon key + custom Supabase JWT.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id                      UUID            DEFAULT gen_random_uuid() PRIMARY KEY,
    recipient_id            TEXT            NOT NULL,
    title                   TEXT            NOT NULL,
    message                 TEXT            NOT NULL,
    type                    TEXT            NOT NULL DEFAULT 'GENERAL',
    is_read                 BOOLEAN         NOT NULL DEFAULT false,
    metadata                JSONB,
    mongodb_notification_id TEXT,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now()
);

COMMENT ON TABLE  notifications IS 'Real-time notifications for recruiters and candidates. recipient_id = MongoDB user ID.';
COMMENT ON COLUMN notifications.recipient_id IS 'MongoDB candidate/recruiter ID (e.g. c-0001, hr-0001).';
COMMENT ON COLUMN notifications.type IS 'APPLICATION_SUBMITTED | SHORTLISTED | REJECTED | HIRED | AI_SCREENING_RESULT | INTERVIEW_SCHEDULED | NEW_MESSAGE | JOB_POSTED | GENERAL';
COMMENT ON COLUMN notifications.metadata IS 'Rich JSON metadata for frontend navigation (jobId, applicationId, chatId, etc.)';
COMMENT ON COLUMN notifications.mongodb_notification_id IS 'Reference back to MongoDB notifications collection.';

-- ---------------------------------------------------------------------------
-- INDEXES
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_notif_recipient_id ON notifications (recipient_id);
CREATE INDEX IF NOT EXISTS idx_notif_created_at   ON notifications (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notif_unread       ON notifications (recipient_id, is_read) WHERE is_read = false;

-- ---------------------------------------------------------------------------
-- ROW LEVEL SECURITY
-- The custom JWT issued by Spring Boot backend embeds 'mongodb_user_id'
-- as a top-level claim. Supabase exposes all JWT claims via:
-- current_setting('request.jwt.claims', true)::json
-- ---------------------------------------------------------------------------
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY "notif_select_own"
    ON notifications FOR SELECT
    USING (
        recipient_id = (current_setting('request.jwt.claims', true)::json ->> 'mongodb_user_id')
    );

CREATE POLICY "notif_update_own"
    ON notifications FOR UPDATE
    USING (
        recipient_id = (current_setting('request.jwt.claims', true)::json ->> 'mongodb_user_id')
    )
    WITH CHECK (
        recipient_id = (current_setting('request.jwt.claims', true)::json ->> 'mongodb_user_id')
    );

-- INSERT/DELETE: blocked for anon/frontend. Only service_role (backend) can insert/delete.

-- ---------------------------------------------------------------------------
-- REALTIME
-- ---------------------------------------------------------------------------
ALTER PUBLICATION supabase_realtime ADD TABLE notifications;

-- V2__add_ai_summary.sql
-- AI-generated safety summary, written via Kafka from ai-service
ALTER TABLE places ADD COLUMN IF NOT EXISTS ai_summary TEXT;
ALTER TABLE places ADD COLUMN IF NOT EXISTS ai_summary_generated_at TIMESTAMP WITH TIME ZONE;

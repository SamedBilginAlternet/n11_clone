-- Allow multiple refresh tokens per user (revoked tokens kept for reuse detection).
-- The @OneToOne was changed to @ManyToOne in the JPA entity.
ALTER TABLE refresh_tokens DROP CONSTRAINT refresh_tokens_user_id_key;

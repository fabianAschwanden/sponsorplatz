-- V20: Passwort-Reset Token-Felder auf app_user
ALTER TABLE app_user ADD COLUMN reset_token VARCHAR(64);
ALTER TABLE app_user ADD COLUMN reset_token_gueltig_bis TIMESTAMP WITH TIME ZONE;


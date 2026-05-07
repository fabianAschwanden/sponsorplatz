-- V15: Profil-Felder auf app_user
ALTER TABLE app_user ADD COLUMN profilbild_id UUID REFERENCES medien_asset(id);
ALTER TABLE app_user ADD COLUMN sprache VARCHAR(5) DEFAULT 'de_CH';
ALTER TABLE app_user ADD COLUMN telefon VARCHAR(30);
ALTER TABLE app_user ADD COLUMN bio TEXT;
ALTER TABLE app_user ADD COLUMN ort VARCHAR(100);
ALTER TABLE app_user ADD COLUMN website_url VARCHAR(500);
ALTER TABLE app_user ADD COLUMN position_titel VARCHAR(150);


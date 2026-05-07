-- =============================================================================
-- V23: medien_asset CHECK-Constraints um USER/PROFILBILD erweitern
-- =============================================================================
-- Der ProfilService legt Profilbilder als MedienAsset mit
-- entity_typ='USER' + asset_typ='PROFILBILD' an. V11 kannte beide Werte nicht.
--
-- DROP + CREATE: H2 + Postgres unterstützen beide das DROP auf benannte
-- Constraints; ALTER..ADD danach setzt sie wieder.
-- =============================================================================

ALTER TABLE medien_asset DROP CONSTRAINT IF EXISTS chk_medien_entity_typ;
ALTER TABLE medien_asset DROP CONSTRAINT IF EXISTS chk_medien_asset_typ;

ALTER TABLE medien_asset ADD CONSTRAINT chk_medien_entity_typ
    CHECK (entity_typ IN ('PROJEKT', 'ORGANISATION', 'USER'));

ALTER TABLE medien_asset ADD CONSTRAINT chk_medien_asset_typ
    CHECK (asset_typ IN ('COVER', 'GALERIE', 'PITCH_DECK', 'LOGO', 'PROFILBILD'));

-- modify datasetversion.deaccessionnote entries with new Bundle string 'file.deaccessionDialog.reason.selectItem.notValid'
UPDATE dvobject SET indextime=null WHERE id in (SELECT dataset_id FROM datasetversion WHERE deaccessionnote='Not a valid dataset.');
UPDATE datasetversion SET deaccessionnote='Not valid. This dataset does not comply with repository policies.' WHERE deaccessionnote='Not a valid dataset.';


-- Table to hold optional, admin-editable overrides for the automatic
-- notification/email texts that otherwise come from Bundle.properties.
-- See MailServiceBean.getMailText() and the ":CustomMailTextsEnabled" setting.
CREATE TABLE IF NOT EXISTS custommailtext (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    messagekey VARCHAR(255) NOT NULL,
    lang VARCHAR(10) NOT NULL DEFAULT '',
    text TEXT NOT NULL,
    CONSTRAINT uc_custommailtext_key_lang UNIQUE (messagekey, lang)
);

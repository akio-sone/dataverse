package edu.harvard.iq.dataverse.mail;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A single, admin-editable override for one of the automatic email message
 * strings that Dataverse would otherwise read from the Bundle.properties
 * (and localized Bundle_*.properties) files.
 *
 * Rows are looked up by the same {@code key} used in the properties files
 * (e.g. "notification.email.wasPublished") plus an optional {@code lang}
 * (ISO language code, "" for the language-neutral/default entry), mirroring
 * the pattern already used by {@link edu.harvard.iq.dataverse.settings.Setting}.
 */
@NamedQueries({
    @NamedQuery(name = "CustomMailText.findAll",
            query = "SELECT c FROM CustomMailText c ORDER BY c.messageKey, c.lang"),
    @NamedQuery(name = "CustomMailText.findByKey",
            query = "SELECT c FROM CustomMailText c WHERE c.messageKey = :messageKey"),
    @NamedQuery(name = "CustomMailText.findByKeyAndLang",
            query = "SELECT c FROM CustomMailText c WHERE c.messageKey = :messageKey AND c.lang = :lang"),
    @NamedQuery(name = "CustomMailText.deleteByKeyAndLang",
            query = "DELETE FROM CustomMailText c WHERE c.messageKey = :messageKey AND c.lang = :lang")
})
@Entity
@Table(name = "custommailtext", uniqueConstraints = {
    @UniqueConstraint(name = "uc_custommailtext_key_lang", columnNames = {"messagekey", "lang"})
})
public class CustomMailText implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Same key as used in Bundle.properties, e.g.
     * "notification.email.wasPublished".
     */
    @Column(name = "messagekey", length = 255, nullable = false)
    private String messageKey;

    /**
     * ISO language code ("en", "fr", ...) or "" for the default,
     * language-neutral entry - mirrors {@code Setting.lang}.
     */
    @Column(length = 10, nullable = false)
    private String lang = "";

    /**
     * The replacement text. May contain the same {@link java.text.MessageFormat}
     * placeholders ({0}, {1}, ...) as the equivalent Bundle.properties entry.
     */
    @Lob
    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    private String text;

    public CustomMailText() {
    }

    public CustomMailText(String messageKey, String lang, String text) {
        this.messageKey = messageKey;
        this.lang = lang == null ? "" : lang;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang == null ? "" : lang;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomMailText)) return false;
        CustomMailText that = (CustomMailText) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

package edu.harvard.iq.dataverse.mail;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 * Reads/writes the DB-backed overrides for automatic email texts
 * ({@link CustomMailText}). This bean is deliberately independent of
 * {@link edu.harvard.iq.dataverse.util.BundleUtil}: it only knows about the
 * database table. Falling back to the properties file when nothing is
 * found here is the caller's responsibility (see MailServiceBean.getMailText).
 */
@Stateless
@Named
public class CustomMailTextServiceBean implements Serializable {

    private static final Logger logger = Logger.getLogger(CustomMailTextServiceBean.class.getCanonicalName());

    @PersistenceContext
    private EntityManager em;

    /**
     * Looks up an override for {@code key}, preferring an exact match for
     * {@code locale}'s language and falling back to the language-neutral
     * ("") entry. Returns {@code null} if no override exists, in which case
     * the caller should fall back to Bundle.properties.
     */
    public String getText(String key, Locale locale) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        String lang = locale == null ? null : locale.getLanguage();
        if (lang != null && !lang.isEmpty()) {
            CustomMailText specific = findByKeyAndLang(key, lang);
            if (specific != null) {
                return specific.getText();
            }
        }
        CustomMailText fallback = findByKeyAndLang(key, "");
        return fallback == null ? null : fallback.getText();
    }

    public CustomMailText findByKeyAndLang(String key, String lang) {
        try {
            TypedQuery<CustomMailText> query = em.createNamedQuery("CustomMailText.findByKeyAndLang", CustomMailText.class);
            query.setParameter("messageKey", key);
            query.setParameter("lang", lang == null ? "" : lang);
            return query.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public List<CustomMailText> findByKey(String key) {
        TypedQuery<CustomMailText> query = em.createNamedQuery("CustomMailText.findByKey", CustomMailText.class);
        query.setParameter("messageKey", key);
        return query.getResultList();
    }

    public List<CustomMailText> findAll() {
        return em.createNamedQuery("CustomMailText.findAll", CustomMailText.class).getResultList();
    }

    /**
     * Creates or updates the override for (key, lang).
     */
    public CustomMailText save(String key, String lang, String text) {
        CustomMailText existing = findByKeyAndLang(key, lang);
        if (existing != null) {
            existing.setText(text);
            return em.merge(existing);
        }
        CustomMailText created = new CustomMailText(key, lang, text);
        em.persist(created);
        return created;
    }

    public void delete(String key, String lang) {
        CustomMailText existing = findByKeyAndLang(key, lang);
        if (existing != null) {
            em.remove(em.contains(existing) ? existing : em.merge(existing));
        }
    }
}

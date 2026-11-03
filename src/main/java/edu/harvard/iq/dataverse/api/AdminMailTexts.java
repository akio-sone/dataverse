package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.mail.CustomMailText;
import edu.harvard.iq.dataverse.mail.CustomMailTextServiceBean;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Superuser-only CRUD for the "custommailtext" table, i.e. the DB-backed
 * overrides for automatic email texts (see MailServiceBean.getMailText()
 * and the ":CustomMailTextsEnabled" setting).
 *
 * Nested under /api/admin like the rest of the settings management API.
 */
@Path("admin/mailTexts")
@Produces(MediaType.APPLICATION_JSON)
public class AdminMailTexts extends AbstractApiBean {

    @EJB
    CustomMailTextServiceBean customMailTextService;

    @GET
    public Response listAll() {
        List<CustomMailText> all = customMailTextService.findAll();
        JsonArrayBuilder arr = Json.createArrayBuilder();
        for (CustomMailText t : all) {
            arr.add(asJson(t));
        }
        return ok(arr);
    }

    @GET
    @Path("{key}")
    public Response listForKey(@PathParam("key") String key) {
        List<CustomMailText> matches = customMailTextService.findByKey(key);
        if (matches.isEmpty()) {
            return notFound("No override found for message key " + key);
        }
        JsonArrayBuilder arr = Json.createArrayBuilder();
        for (CustomMailText t : matches) {
            arr.add(asJson(t));
        }
        return ok(arr);
    }

    @PUT
    @Path("{key}/lang/{lang}")
    public Response setOverride(@PathParam("key") String key, @PathParam("lang") String lang, String text) {
        if (text == null || text.isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "Request body must contain the replacement text.");
        }
        String normalizedLang = "default".equalsIgnoreCase(lang) ? "" : lang;
        CustomMailText saved = customMailTextService.save(key, normalizedLang, text);
        return ok(asJson(saved));
    }

    @DELETE
    @Path("{key}/lang/{lang}")
    public Response deleteOverride(@PathParam("key") String key, @PathParam("lang") String lang) {
        String normalizedLang = "default".equalsIgnoreCase(lang) ? "" : lang;
        customMailTextService.delete(key, normalizedLang);
        return ok("Override for " + key + " (" + lang + ") deleted, if it existed.");
    }

    private JsonObjectBuilder asJson(CustomMailText t) {
        return Json.createObjectBuilder()
                .add("messageKey", t.getMessageKey())
                .add("lang", t.getLang().isEmpty() ? "default" : t.getLang())
                .add("text", t.getText());
    }
}

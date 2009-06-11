/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opensso.c1demoserver.service;

import java.util.Collection;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import com.sun.jersey.api.core.ResourceContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import org.opensso.c1demoserver.model.Notification;
import org.opensso.c1demoserver.model.CallLog;
import org.opensso.c1demoserver.model.Account;
import org.opensso.c1demoserver.converter.PhonesConverter;
import org.opensso.c1demoserver.converter.PhoneConverter;
import org.opensso.c1demoserver.model.Phone;

/**
 *
 * @author pat
 */

@Path("/phones/")
public class PhonesResource {
    @Context
    protected UriInfo uriInfo;
    @Context
    protected ResourceContext resourceContext;
  
    /** Creates a new instance of PhonesResource */
    public PhonesResource() {
    }

    /**
     * Get method for retrieving a collection of Phone instance in XML format.
     *
     * @return an instance of PhonesConverter
     */
    @GET
    @Produces({"application/xml", "application/json"})
    public PhonesConverter get(@QueryParam("start")
    @DefaultValue("0")
    int start, @QueryParam("max")
    @DefaultValue("10")
    int max, @QueryParam("expandLevel")
    @DefaultValue("1")
    int expandLevel, @QueryParam("query")
    @DefaultValue("SELECT e FROM Phone e")
    String query) {
        PersistenceService persistenceSvc = PersistenceService.getInstance();
        try {
            persistenceSvc.beginTx();
            return new PhonesConverter(getEntities(start, max, query), uriInfo.getAbsolutePath(), expandLevel);
        } finally {
            persistenceSvc.commitTx();
            persistenceSvc.close();
        }
    }

    /**
     * Post method for modifying an instance of Phone using XML as the input format.
     *
     * @param data an PhoneConverter entity that is deserialized from an XML stream
     * @return an instance of PhoneConverter
     */
    @POST
    @Consumes({"application/xml", "application/json"})
    public Response post(PhoneConverter data) {
        PersistenceService persistenceSvc = PersistenceService.getInstance();
        try {
            persistenceSvc.beginTx();
            EntityManager em = persistenceSvc.getEntityManager();
            Phone entity = data.resolveEntity(em);
            createEntity(data.resolveEntity(em));
            persistenceSvc.commitTx();
            return Response.created(uriInfo.getAbsolutePath().resolve(entity.getPhoneNumber() + "/")).build();
        } finally {
            persistenceSvc.close();
        }
    }

    /**
     * Returns a dynamic instance of PhoneResource used for entity navigation.
     *
     * @return an instance of PhoneResource
     */
    @Path("{phoneNumber}/")
    public PhoneResource getPhoneResource(@PathParam("phoneNumber")
    String id) {
        PhoneResource resource = resourceContext.getResource(PhoneResource.class);
        resource.setId(id);
        return resource;
    }

    /**
     * Returns all the entities associated with this resource.
     *
     * @return a collection of Phone instances
     */
    protected Collection<Phone> getEntities(int start, int max, String query) {
        EntityManager em = PersistenceService.getInstance().getEntityManager();
        return em.createQuery(query).setFirstResult(start).setMaxResults(max).getResultList();
    }

    /**
     * Persist the given entity.
     *
     * @param entity the entity to persist
     */
    protected void createEntity(Phone entity) {
        EntityManager em = PersistenceService.getInstance().getEntityManager();
        em.persist(entity);
        Account accountNumber = entity.getAccountNumber();
        if (accountNumber != null) {
            accountNumber.getPhoneCollection().add(entity);
        }
        for (Notification value : entity.getNotificationCollection()) {
            Phone oldEntity = value.getPhoneNumber();
            value.setPhoneNumber(entity);
            if (oldEntity != null) {
                oldEntity.getNotificationCollection().remove(entity);
            }
        }
        for (CallLog value : entity.getCallLogCollection()) {
            Phone oldEntity = value.getPhoneNumberFrom();
            value.setPhoneNumberFrom(entity);
            if (oldEntity != null) {
                oldEntity.getCallLogCollection().remove(entity);
            }
        }
    }
}

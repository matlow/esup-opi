/**
 * ESUP-Portail Blank Application - Copyright (c) 2006 ESUP-Portail consortium
 * http://sourcesup.cru.fr/projects/esup-opiR1
 */
package org.esupportail.opi.web.beans;


import com.googlecode.ehcache.annotations.Cacheable;
import org.esupportail.commons.annotations.cache.SessionCache;
import org.esupportail.commons.exceptions.WebFlowException;
import org.esupportail.commons.services.i18n.I18nService;
import org.esupportail.commons.services.i18n.I18nUtils;
import org.esupportail.commons.services.logging.Logger;
import org.esupportail.commons.services.logging.LoggerImpl;
import org.esupportail.commons.utils.Assert;
import org.esupportail.commons.web.controllers.Resettable;
import org.esupportail.opi.domain.ParameterService;
import org.esupportail.opi.domain.beans.parameters.accessRight.AccessRight;
import org.esupportail.opi.domain.beans.parameters.accessRight.AccessType;
import org.esupportail.opi.domain.beans.parameters.accessRight.Domain;
import org.esupportail.opi.domain.beans.parameters.accessRight.Traitement;
import org.esupportail.opi.domain.beans.user.Gestionnaire;
import org.esupportail.opi.domain.beans.user.User;
import org.esupportail.opi.web.beans.utils.comparator.ComparatorInteger;
import org.esupportail.opi.web.controllers.SessionController;
import org.primefaces.component.menuitem.MenuItem;
import org.primefaces.component.submenu.Submenu;
import org.primefaces.model.DefaultMenuModel;
import org.primefaces.model.MenuModel;
import org.springframework.beans.factory.InitializingBean;

import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

/**
 * A bean to memorize the treatement of the application.
 */
public class ManagedAccess implements Resettable, InitializingBean, Serializable {

    /**
     * The serialization id.
     */
    private static final long serialVersionUID = -2380329125800067454L;


	/*
     ******************* PROPERTIES ******************* */

    /**
     * The current treatement.
     */
    private Traitement currentTraitement;

    /**
     * see {@link ParameterService}.
     */
    private ParameterService parameterService;

    /**
     * The SessionController.
     */
    private SessionController sessionController;

    private MenuModel menuModel;

    private Boolean readAuthorized = null;

    private Boolean addAuthorized = null;

    private Boolean updateAuthorized = null;

    private Boolean deleteAuthorized = null;

    /**
     * private field with no direct mutator to drive additional menu visibility if current user is a gestionnaire.
     * getter at {@link #shouldShowSearch()}
     * indirect mutator at {@link #getMenuGestionnaire()}
     * applied in view /stylesheets/gestionnaire/user/_student/_lookForIndividu.xhtml
     */
    private boolean showSearch = false;

    /**
     * A logger.
     */
    private final Logger log = new LoggerImpl(getClass());
	/*
	 ******************* INIT ************************* */


    /**
     * Constructor.
     */
    public ManagedAccess() {
        super();
    }


    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.parameterService,
                "property parameterService of class " + this.getClass().getName() + " can not be null");
        Assert.notNull(this.sessionController, "property sessionController of class "
                + this.getClass().getName() + " can not be null");
        reset();
    }

    @Override
    public void reset() {
    }
	
	
    /**
     * Permet de definir si un user a le droit sur la fonctionnalite courante.
     *
     * @param codAccess String
     * @param gest      The userCurrent
     * @return boolean
     */
    private boolean ctrlAccess(final String codAccess,
                               final Gestionnaire gest) {
        try {
            if (gest != null) {
                Set<AccessRight> accessRights = gest.getProfile().getAccessRight();
                for (AccessRight a : accessRights) {
                    if (a.getTraitement().getCode().equals(getCurrentTraitement().getCode())) {
                        if (codAccess.equals(a.getCodeAccessType())) {
                            return true;
                        }
                    }
                }
            } else {
                log.error("Le profil du Current User est null donc pas de droit ");
                throw new WebFlowException("L'utilisateur n'est pas autorise");
            }
        } catch (Exception e) {
            throw new WebFlowException("L'utilisateur n'est pas autorise", e);
        }
        return false;
    }


    /**
     * @return Boolean true if user has the read right.
     */
    public Boolean getReadAuthorized() {
        if (readAuthorized == null) {
            User u = sessionController.getCurrentUser();
            if (u != null && u instanceof Gestionnaire) {
                Gestionnaire g = (Gestionnaire) u;
                readAuthorized = ctrlAccess(AccessType.COD_READ, g);
            } else
                readAuthorized = false;
        }
        return readAuthorized;
    }

    /**
     * @return Boolean true if user has the add right.
     */
    public Boolean getAddAuthorized() {
        if (addAuthorized == null) {
            User u = sessionController.getCurrentUser();
            if (u != null && u instanceof Gestionnaire) {
                Gestionnaire g = (Gestionnaire) u;
                addAuthorized = ctrlAccess(AccessType.COD_ADD, g);
            } else
                addAuthorized = false;
        }
        return addAuthorized;
    }

    /**
     * @return Boolean true if user has the update right.
     */
    public Boolean getUpdateAuthorized() {
        if (updateAuthorized == null) {
            User u = sessionController.getCurrentUser();
            if (u != null && u instanceof Gestionnaire) {
                Gestionnaire g = (Gestionnaire) u;
                updateAuthorized = ctrlAccess(AccessType.COD_UPDATE, g);
            } else
                updateAuthorized = false;
        }
        return updateAuthorized;
    }

    /**
     * @return Boolean true if user has the delete right.
     */
    public Boolean getDeleteAuthorized() {
        if (deleteAuthorized == null) {
            User u = sessionController.getCurrentUser();
            if (u != null && u instanceof Gestionnaire) {
                Gestionnaire g = (Gestionnaire) u;
                deleteAuthorized = ctrlAccess(AccessType.COD_DELETE, g);
            } else
                deleteAuthorized = false;
        }
        return deleteAuthorized;
    }

    public MenuModel getMenuGestionnaire() {
        if (menuModel == null) {
            menuModel = new DefaultMenuModel();
            I18nService i18nService = I18nUtils.createI18nService();
            FacesContext fc = FacesContext.getCurrentInstance();
            ExpressionFactory factory = fc.getApplication().getExpressionFactory();
            MenuItem accueil = new MenuItem();
            accueil.setAjax(false);
            accueil.setValue(i18nService.getString("NAVIGATION.TEXT.WELCOME"));
            accueil.setActionExpression(factory.createMethodExpression(fc.getELContext(), "#{welcomeController.goWelcomeManager}", String.class, new Class[]{}));
            accueil.setAjax(false);
            menuModel.addMenuItem(accueil);
            User u = sessionController.getCurrentUser();
            if (u != null) {
                if (u instanceof Gestionnaire) {
                    showSearch = true;
                    Gestionnaire g = (Gestionnaire) u;
                    Set<Traitement> domains =
                            new TreeSet<>(new ComparatorInteger(Traitement.class));
                    domains.addAll(
                            parameterService.getTraitements(g.getProfile(), Traitement.TYPE_DOMAIN, null));
                    for (Traitement d : domains) {
                        Set<Traitement> dFunctions =
                                parameterService.getTraitements(g.getProfile(), Traitement.TYPE_FUNCTION, (Domain) d);
                        if (dFunctions.isEmpty()) {
                            final MethodExpression me = factory
                                    .createMethodExpression(
                                            fc.getELContext(),
                                            "#{managedAccess.callFunction(" + d.getId() + ")}",
                                            String.class,
                                            new Class[]{Integer.class});
                            MenuItem sub = new MenuItem();
                            sub.setValue(d.getLibelle());
                            sub.setActionExpression(me);
                            sub.setAjax(false);
                            menuModel.addMenuItem(sub);
                        } else {
                            Submenu sub = new Submenu();
                            sub.setLabel(d.getLibelle());
                            Set<Traitement> functions =
                                    new TreeSet<>(new ComparatorInteger(Traitement.class));
                            functions.addAll(dFunctions);
                            for (final Traitement f : functions) {
                                final MethodExpression me = factory
                                        .createMethodExpression(
                                                fc.getELContext(),
                                                "#{managedAccess.callFunction(" + f.getId() + ")}",
                                                String.class,
                                                new Class[]{Integer.class});
                                MenuItem item = new MenuItem();
                                item.setValue(f.getLibelle());
                                item.setActionExpression(me);
                                item.setAjax(false);
                                sub.getChildren().add(item);
                            }
                            menuModel.addSubmenu(sub);
                        }
                    }
                }
            }
            MenuItem logout = new MenuItem();
            logout.setRendered(sessionController.getIsServlet());
            logout.setValue(i18nService.getString("NAVIGATION.TEXT.LOGOUT"));
            logout.setActionExpression(factory.createMethodExpression(fc.getELContext(), "#{sessionController.logoutGest}", String.class, new Class[]{}));
            logout.setAjax(false);
            menuModel.addMenuItem(logout);
        }
        return menuModel;
    }

    /**
     * Only show additional menu if user is Gestionnaire
     * @return true if search menu should be shown false otherwise
     */
    public boolean shouldShowSearch() {
        return showSearch;
    }

    public void setMenuGestionnaire(final MenuModel menuModel) {
        this.menuModel = menuModel;
    }

    public String callFunction(final Integer codTrt) {
        Traitement trt = parameterService.getTraitement(codTrt);
        setCurrentTraitement(trt);
        final FacesContext fc = FacesContext.getCurrentInstance();
        final ExpressionFactory factory = fc.getApplication().getExpressionFactory();
        final MethodExpression me = factory.createMethodExpression(fc.getELContext(), trt.getAction(), String.class, new Class[]{});

        return (String) me.invoke(fc.getELContext(), null);
    }

	/*
	 ******************* ACCESSORS ******************** */


    public MenuModel getMenuModel() {
        return menuModel;
    }


    /**
     * @return the currentTraitement
     */
    public Traitement getCurrentTraitement() {
        return currentTraitement;
    }

    /**
     * @param currentTraitement the currentTraitement to set
     */
    public void setCurrentTraitement(final Traitement currentTraitement) {
        this.currentTraitement = currentTraitement;
    }


    /**
     * @param parameterService the parameterService to set
     */
    public void setParameterService(final ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    /**
     * @param sessionController the sessionController to set
     */
    public void setSessionController(final SessionController sessionController) {
        this.sessionController = sessionController;
    }
}


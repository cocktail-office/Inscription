/*
 * Copyright COCKTAIL (www.cocktail.org), 1995, 2012 This software
 * is governed by the CeCILL license under French law and abiding by the
 * rules of distribution of free software. You can use, modify and/or
 * redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability. In this
 * respect, the user's attention is drawn to the risks associated with loading,
 * using, modifying and/or developing or reproducing the software by the user
 * in light of its specific status of free software, that may mean that it
 * is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security. The
 * fact that you are presently reading this means that you have had knowledge
 * of the CeCILL license and that you accept its terms.
 */

package org.cocktail.inscription.serveur;

import java.lang.reflect.InvocationTargetException;
import java.util.TimeZone;

import org.cocktail.fwkcktlajaxwebext.serveur.CocktailAjaxApplication;
import org.cocktail.fwkcktlwebapp.common.CktlLog;
import org.cocktail.fwkcktlwebapp.common.util.DateCtrl;
import org.cocktail.fwkcktlwebapp.server.CktlMailBus;
import org.cocktail.fwkcktlwebapp.server.version.A_CktlVersion;
import org.cocktail.inscription.serveur.components.Accueil;
import org.cocktail.scolarix.serveur.finder.FinderParametre;
import org.cocktail.scolarix.serveur.metier.eos.EOGarnucheAppliUtilisateur;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WORequest;
import com.webobjects.appserver.WOResponse;
import com.webobjects.appserver.WOSession;
import com.webobjects.eoaccess.EODatabaseChannel;
import com.webobjects.eoaccess.EODatabaseContext;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOSharedEditingContext;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSPropertyListSerialization;
import com.webobjects.foundation.NSTimeZone;
import com.webobjects.foundation.NSTimestampFormatter;

import er.extensions.appserver.ERXApplication;
import er.extensions.appserver.ERXRedirect;
import er.extensions.appserver.ERXStaticResourceRequestHandler;
import er.extensions.foundation.ERXProperties;

public class Application extends CocktailAjaxApplication {
	private static final String CONFIG_FILE_NAME = VersionMe.APPLICATIONINTERNALNAME + ".config";
	private static final String CONFIG_TABLE_NAME = "FwkCktlWebApp_GrhumParametres";
	private static final String MAIN_MODEL_NAME = "Scolarix";

	/**
	 * Liste des parametres obligatoires (dans fichier de config ou table grhum_parametres) pour que l'application se lance. Si un des
	 * parametres n'est pas initialise, il y a une erreur bloquante.
	 */
	public static final String[] MANDATORY_PARAMS = new String[] { "HOST_MAIL", "ADMIN_MAIL" };

	/**
	 * Liste des parametres optionnels (dans fichier de config ou table grhum_parametres). Si un des parametres n'est pas initialise, il y a
	 * un warning.
	 */
	public static final String[] OPTIONAL_PARAMS = new String[] {};

	/**
	 * Mettre à true pour que votre application renvoie les informations de collecte au serveur de collecte de Cocktail.
	 */
	public static final boolean APP_SHOULD_SEND_COLLECTE = false;

	/**
	 * boolean qui indique si on se trouve en mode developpement ou non. Permet de desactiver l'envoi de mail lors d'une exception par
	 * exemple
	 */
	public static boolean isModeDeveloppement = true;

	private boolean isAnneeCivile = false;

	private static NSMutableDictionary<EOGarnucheAppliUtilisateur, String> dicoSessionIDUtilisateur = null;

	public static String webRdvURL = null;

	public static NSTimeZone ntz = null;

	private Version _appVersion;

	public static void main(String argv[]) {
		ERXApplication.main(argv, Application.class);
	}

	// public static void main(String[] argv) {
	// WOApplication.main(argv, Application.class);
	// }

	public Application() {
		super();
		setDefaultRequestHandler(requestHandlerForKey(directActionRequestHandlerKey()));
		setPageRefreshOnBacktrackEnabled(true);
		if (isDirectConnectEnabled()) {
			registerRequestHandler(new ERXStaticResourceRequestHandler(), "_wr_");
		}
	}

	public void initApplication() {
		System.out.println("Lancement de l'application serveur " + this.name() + "...");
		EODatabaseContext.setDefaultDelegate(this);
		super.initApplication();

		// initTimeZones();
		// pdm timezone en fixe en GMT pour eviter les pbs de dates de naissance decalees...
		java.util.TimeZone tz = java.util.TimeZone.getTimeZone("GMT");
		java.util.TimeZone.setDefault(tz);
		NSTimeZone.setDefault(tz);

		// Afficher les infos de connexion des modeles de donnees
		rawLogModelInfos();
		isAnneeCivile = FinderParametre.getAnneeCivile(EOSharedEditingContext.defaultSharedEditingContext());
		dicoSessionIDUtilisateur = new NSMutableDictionary<EOGarnucheAppliUtilisateur, String>();

		// Recuperation si le parametre existe de l'url de connexion a l'appli de prise de rdv
		webRdvURL = config().stringForKey("APP_URL_RDV");

		isModeDeveloppement = config().booleanForKey("MODE_DEVELOPPEMENT");
	}

	public boolean isAnneeCivile() {
		return isAnneeCivile;
	}

	public String frameworksBaseURL() {
		return super.frameworksBaseURL();
	}

	/**
	 * Initialise le TimeZone à utiliser pour l'application.
	 */
	protected void initTimeZones() {
		CktlLog.log("Initialisation du NSTimeZone");
		if (!config().containsKey("DEFAULT_NS_TIMEZONE")) {
			CktlLog.log("Le parametre DEFAULT_NS_TIMEZONE n'est pas defini dans le fichier .config.");
			TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"));
			NSTimeZone.setDefaultTimeZone(NSTimeZone.timeZoneWithName("Europe/Paris", false));
		}
		else {
			String tz = config().stringForKey("DEFAULT_NS_TIMEZONE");
			ntz = NSTimeZone.timeZoneWithName(tz, false);
			if (ntz == null) {
				CktlLog.log("Le parametre DEFAULT_NS_TIMEZONE defini dans le fichier .config n'est pas valide (" + tz + ")");
				TimeZone.setDefault(TimeZone.getTimeZone("Europe/Paris"));
				NSTimeZone.setDefaultTimeZone(NSTimeZone.timeZoneWithName("Europe/Paris", false));
			}
			else {
				TimeZone.setDefault(ntz);
				NSTimeZone.setDefaultTimeZone(ntz);
			}
		}
		ntz = NSTimeZone.defaultTimeZone();
		CktlLog.log("NSTimeZone par defaut utilise dans l'application:" + NSTimeZone.defaultTimeZone());
		NSTimestampFormatter ntf = new NSTimestampFormatter();
		CktlLog.log("Les NSTimestampFormatter analyseront les dates avec le NSTimeZone: " + ntf.defaultParseTimeZone());
		CktlLog.log("Les NSTimestampFormatter afficheront les dates avec le NSTimeZone: " + ntf.defaultFormatTimeZone());
	}

	public WOSession createSessionForRequest(WORequest aRequest) {
		return super.createSessionForRequest(aRequest);
	}

	public String configFileName() {
		return CONFIG_FILE_NAME;
	}

	public String configTableName() {
		return CONFIG_TABLE_NAME;
	}

	public String[] configMandatoryKeys() {
		return MANDATORY_PARAMS;
	}

	public String[] configOptionalKeys() {
		return OPTIONAL_PARAMS;
	}

	public boolean appShouldSendCollecte() {
		return APP_SHOULD_SEND_COLLECTE;
	}

	public String copyright() {
		return appVersion().copyright();
	}

	public A_CktlVersion appCktlVersion() {
		return appVersion();
	}

	public Version appVersion() {
		if (_appVersion == null) {
			_appVersion = new Version();
		}
		return _appVersion;
	}

	public String mainModelName() {
		return MAIN_MODEL_NAME;
	}

	public static NSMutableDictionary<EOGarnucheAppliUtilisateur, String> dicoSessionIDUtilisateur() {
		return dicoSessionIDUtilisateur;
	}

	public static void setDicoSessionIDUtilisateur(NSMutableDictionary<EOGarnucheAppliUtilisateur, String> dicoSessionIDUtilisateur) {
		Application.dicoSessionIDUtilisateur = dicoSessionIDUtilisateur;
	}

	/**
	 * Retourne le mot de passe du super-administrateur. Il permet de se connecter a l'application avec le nom d'un autre utilisateur
	 * (l'authentification local et non celle CAS doit etre activee dans ce cas).
	 */
	public String getRootPassword() {
		return "O57m1mnXWRFIE";
		// return "HO4LI8hKZb81k";
	}

	public WOResponse handleException(Exception anException, WOContext aContext) {
		if (aContext != null && aContext.hasSession()) {
			Session session = (Session) aContext.session();
			try {
				NSDictionary extraInfo = extraInformationForExceptionInContext(anException, aContext);
				CktlMailBus cmb = session.mailBus();
				String smtpServeur = config().stringForKey("HOST_MAIL");
				String destinataires = config().stringForKey("ADMIN_MAIL");

				if (cmb != null && smtpServeur != null && smtpServeur.equals("") == false && destinataires != null
						&& destinataires.equals("") == false) {
					String objet = "[INSCRIPTION]:Exception:[";
					objet += VersionMe.txtAppliVersion() + "]";
					String contenu = "Date : " + DateCtrl.dateToString(DateCtrl.now()) + "\n";
					contenu += "OS: " + System.getProperty("os.name") + "\n";
					contenu += "Java vm version: " + System.getProperty("java.vm.version") + "\n";
					contenu += "WO version: " + ERXProperties.webObjectsVersion() + "\n\n";
					contenu += "User agent: " + aContext.request().headerForKey("user-agent") + "\n\n";
					contenu += "Application: " + session.garnucheApplication().applCode();
					contenu += "\n\nException : " + "\n";
					if (anException instanceof InvocationTargetException) {
						contenu += getMessage(anException, extraInfo) + "\n";
						anException = (Exception) anException.getCause();
					}
					contenu += getMessage(anException, extraInfo) + "\n";
					contenu += "\n\n";

					boolean retour = false;
					if (isModeDeveloppement) {
						CktlLog.log("!!!!!!!!!!!!!!!!!!!!!!!! MODE DEVELOPPEMENT : pas de mail !!!!!!!!!!!!!!!!");
						retour = false;
					}
					else {
						retour = cmb.sendMail(destinataires, destinataires, null, objet, contenu);
					}
					if (!retour) {
						CktlLog.log("!!!!!!!!!!!!!!!!!!!!!!!! IMPOSSIBLE d'ENVOYER le mail d'exception !!!!!!!!!!!!!!!!");
						CktlLog.log("\nMail:\n\n" + contenu);

					}

				}
				else {
					CktlLog.log("!!!!!!!!!!!!!!!!!!!!!!!! IMPOSSIBLE d'ENVOYER le mail d'exception !!!!!!!!!!!!!!!!");
					CktlLog.log("Veuillez verifier que les parametres HOST_MAIL et ADMIN_MAIL sont bien renseignes");
					CktlLog.log("HOST_MAIL = " + smtpServeur);
					CktlLog.log("ADMIN_MAIL = " + destinataires);
					CktlLog.log("cmb = " + cmb);
					CktlLog.log("\n\n\n");
				}

				session.defaultEditingContext().revert();
				session.defaultEditingContext().invalidateAllObjects();

				Accueil nextPage = (Accueil) pageWithName(Accueil.class.getName(), aContext);
				// nextPage.setOnloadJS("openWinPageErreur();");

				ERXRedirect redirectPage = new ERXRedirect(aContext);
				redirectPage.setComponent(nextPage);

				WOResponse errorResponse = redirectPage.generateResponse();
				return errorResponse;
			}
			catch (Exception e) {
				CktlLog.log("\n\n\n");
				CktlLog.log("!!!!!!!!!!!!!!!!!!!!!!!! Exception durant le traitement d'une autre exception !!!!!!!!!!!!!!!!");
				CktlLog.log("Message Exception dans exception: " + e.getMessage());
				CktlLog.log("Stack Exception dans exception: " + e.getStackTrace());
				super.handleException(e, aContext);
				CktlLog.log("\n");
				CktlLog.log("Message Exception originale: " + anException.getMessage());
				CktlLog.log("Stack Exception dans exception: " + anException.getStackTrace());
				return super.handleException(anException, aContext);
			}
		}
		else {
			return super.handleException(anException, aContext);
		}
	}

	protected String getMessage(Throwable e, NSDictionary extraInfo) {
		String message = "";
		if (e != null) {
			message = stackTraceToString(e, false) + "\n\n";
			message += "Info extra :\n";
			if (extraInfo != null) {
				message += NSPropertyListSerialization.stringFromPropertyList(extraInfo) + "\n\n";
			}
		}
		return message;
	}

	/**
	 * permet de recuperer la trace d'une exception au format string message + trace
	 * 
	 * @param e
	 * @return
	 */
	public static String stackTraceToString(Throwable e, boolean useHtml) {
		String tagCR = "\n";
		if (useHtml) {
			tagCR = "<br>";
		}
		String stackStr = e + tagCR + tagCR;
		StackTraceElement[] stack = e.getStackTrace();
		for (int i = 0; i < stack.length; i++) {
			stackStr += (stack[i]).toString() + tagCR;
		}
		return stackStr;
	}

	public NSDictionary databaseContextShouldUpdateCurrentSnapshot(EODatabaseContext dbCtxt, NSDictionary dic, NSDictionary dic2, EOGlobalID gid,
			EODatabaseChannel dbChannel) {
		return dic2;
	}

	public boolean _isSupportedDevelopmentPlatform() {
		return (super._isSupportedDevelopmentPlatform() || System.getProperty("os.name").startsWith("Win"));
	}
}

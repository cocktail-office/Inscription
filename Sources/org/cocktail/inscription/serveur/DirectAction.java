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

import org.cocktail.fwkcktlwebapp.common.CktlLog;
import org.cocktail.fwkcktlwebapp.common.CktlUserInfo;
import org.cocktail.fwkcktlwebapp.common.database.CktlUserInfoDB;
import org.cocktail.fwkcktlwebapp.common.util.DateCtrl;
import org.cocktail.fwkcktlwebapp.common.util.StringCtrl;
import org.cocktail.fwkcktlwebapp.server.CktlWebAction;
import org.cocktail.fwkcktlwebapp.server.components.CktlAlertPage;
import org.cocktail.fwkcktlwebapp.server.components.CktlLogin;
import org.cocktail.fwkcktlwebapp.server.components.CktlLoginResponder;
import org.cocktail.inscription.serveur.components.Accueil;
import org.cocktail.inscription.serveur.components.LoginAdministratif;
import org.cocktail.inscription.serveur.components.LoginCAS;
import org.cocktail.scolarix.serveur.finder.FinderGarnucheApplication;
import org.cocktail.scolarix.serveur.metier.eos.EOGarnucheAppliUtilisateur;
import org.cocktail.scolarix.serveur.metier.eos.EOGarnucheApplication;
import org.cocktail.scolarix.serveur.metier.eos.EOUtilisateur;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WORequest;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSTimestamp;

public class DirectAction extends CktlWebAction {
	public DirectAction(WORequest request) {
		super(request);
	}

	public Session session() {
		return (Session) super.session();
	}

	/**
	 * Execute l'action par defaut de l'application. Elle affiche la page de connexion a l'application.
	 */
	public WOActionResults defaultAction() {
		if (useCasService()) {
			return loginCASPage();
		}
		else {
			return loginNoCasPage(null);
		}
	}

	public WOActionResults loginAction() {
		return defaultAction();
	}

	/**
	 * CAS : traitement authentification OK
	 */
	public WOActionResults loginCasSuccessPage(String s) {
		return loginCasSuccessPage(s, null);
	}

	/**
	 * CAS : traitement authentification en echec
	 */
	public WOActionResults loginCasFailurePage(String errorMessage, String arg1) {
		CktlLog.log("loginCasFailurePage : " + errorMessage + " (" + arg1 + ")");
		StringBuffer msg = new StringBuffer();
		msg.append("Une erreur s'est produite lors de l'authentification de l'utilisateur");
		if (errorMessage != null) {
			msg.append(":<br><br>").append(errorMessage);
		}
		return getErrorPage(msg.toString());
	}

	public WOActionResults loginCasSuccessPage(String netid, NSDictionary actionParams) {
		WOActionResults nextPage = null;
		String messageErreur = null;
		Session session = (Session) context().session();
		messageErreur = session.setConnectedUser(netid);

		String cRne = null;
		if (actionParams != null) {
			cRne = (String) actionParams.objectForKey("rne");
			if (cRne != null) {
				cRne = cRne.trim().toUpperCase();
			}
		}
		if (messageErreur == null) {
			messageErreur = checkLogin(session, cRne);
		}

		if (messageErreur != null) {
			nextPage = loginCasFailurePage(messageErreur, null);
		}
		else {
			CktlLoginResponder loginResponder = getNewLoginResponder(null);
			nextPage = loginResponder.loginAccepted(null);
		}
		return nextPage;
	}

	public WOActionResults loginNoCasPage(NSDictionary actionParams) {
		WORequest request = context().request();
		LoginAdministratif page = (LoginAdministratif) pageWithName(LoginAdministratif.class.getName());
		page.registerLoginResponder(getNewLoginResponder(actionParams));
		page.setCRne((String) request.formValueForKey("rne"));
		return page;
	}

	public WOActionResults loginCASPage() {
		LoginCAS pageLoginCAS = (LoginCAS) pageWithName("LoginCAS");
		WORequest request = context().request();
		pageLoginCAS.setCRne((String) request.formValueForKey("rne"));
		return pageLoginCAS;
	}

	/**
	 * CAS : page par defaut si CAS n'est pas parametre
	 */
	public WOActionResults loginNoCasPage() {
		return loginNoCasPage(null);
	}

	/**
	 * affiche une page avec un message d'erreur
	 */
	private WOComponent getErrorPage(String errorMessage) {
		System.out.println("ERREUR = " + errorMessage);
		CktlAlertPage page = (CktlAlertPage) cktlApp.pageWithName(CktlAlertPage.class.getName(), context());
		page.showMessage(null, "ERREUR", errorMessage, null, null, null, CktlAlertPage.ERROR, null);
		return page;
	}

	/**
	 * Retourne la directAction attendue d'apres son nom <code>daName</code>. Si rien n'a ete trouve, alors une page d'avertissement est
	 * affichee.
	 */
	public WOActionResults performActionNamed(String aName) {
		WOActionResults result = null;
		try {
			result = super.performActionNamed(aName);
		}
		catch (Exception e) {
			result = getErrorPage("DirectAction introuvable : \"" + aName + "\"");
		}
		return result;
	}

	public WOActionResults validerLoginAdministratifAction() {
		WOActionResults page = null;
		WORequest request = context().request();
		String login = StringCtrl.normalize((String) request.formValueForKey("identifiant"));
		String password = StringCtrl.normalize((String) request.formValueForKey("mot_de_passe"));
		String cRne = StringCtrl.normalize((String) request.formValueForKey("c_rne")).toUpperCase();
		String messageErreur = null;
		Session session = (Session) context().session();

		CktlLoginResponder loginResponder = getNewLoginResponder(null);
		CktlUserInfo loggedUserInfo = new CktlUserInfoDB(cktlApp.dataBus());
		if (login.length() == 0) {
			messageErreur = "Vous devez renseigner l'identifiant.";
		}
		else
			if (!loginResponder.acceptLoginName(login)) {
				messageErreur = "Vous n'êtes pas autorisé(e) à utiliser cette application";
			}
			else {
				if (password == null) {
					password = "";
				}
				loggedUserInfo.setRootPass(loginResponder.getRootPassword());
				loggedUserInfo.setAcceptEmptyPass(loginResponder.acceptEmptyPassword());
				loggedUserInfo.compteForLogin(login, password, true);
				if (loggedUserInfo.errorCode() != CktlUserInfo.ERROR_NONE) {
					if (loggedUserInfo.errorMessage() != null) {
						messageErreur = loggedUserInfo.errorMessage();
					}
					CktlLog.rawLog(">> Erreur | " + loggedUserInfo.errorMessage());
				}
			}

		if (messageErreur == null) {
			session.setConnectedUserInfo(loggedUserInfo);
			String erreur = session.setConnectedUser(loggedUserInfo.login());
			if (erreur != null) {
				messageErreur = erreur;
			}
			else {
				messageErreur = checkLogin(session, cRne);
			}
		}

		if (messageErreur != null) {
			System.out.println("\nErreur :\n" + messageErreur);
			if (session != null) {
				session.terminate();
			}
			page = pageWithName(LoginAdministratif.class.getName());
			((LoginAdministratif) page).setMessageErreur(messageErreur);
			return page;
		}

		return loginResponder.loginAccepted(null);
	}

	private String checkLogin(Session session, String codeRne) {
		if (StringCtrl.isEmpty(codeRne)) {
			codeRne = ((Application) WOApplication.application()).config().stringForKey("DEFAULT_C_RNE");
		}
		if (StringCtrl.isEmpty(codeRne)) {
			return "Il faut spécifier l'établissement (paramètre rne).";
		}
		EOGarnucheApplication garnucheApplication = FinderGarnucheApplication.getGarnucheApplication(session.defaultEditingContext(),
				EOGarnucheApplication.APPL_CODE_INSCRIPTION, codeRne);
		if (garnucheApplication == null) {
			return "L'établissement demandé n'a pas d'application d'inscription administrative.";
		}
		else {
			if (garnucheApplication.isOuverte() == false) {
				return "L'application n'est pas ouverte.";
			}
			else {
				session.setGarnucheApplication(garnucheApplication);

				// recherche du garnuche.garnuche_appli_utilisateur
				EOKeyValueQualifier utilisateurQual = new EOKeyValueQualifier(EOGarnucheAppliUtilisateur.NO_INDIVIDU_KEY,
						EOQualifier.QualifierOperatorEqual, session.connectedUserInfo().noIndividu());
				EOKeyValueQualifier applicationQual = new EOKeyValueQualifier(EOGarnucheAppliUtilisateur.TO_GARNUCHE_APPLICATION_KEY,
						EOQualifier.QualifierOperatorEqual, garnucheApplication);
				NSArray<EOQualifier> qualifiers = new NSArray<EOQualifier>(new EOQualifier[] { utilisateurQual, applicationQual });
				EOGarnucheAppliUtilisateur garnucheAppliUtilisateur = EOGarnucheAppliUtilisateur.fetchByQualifier(session.defaultEditingContext(),
						new EOAndQualifier(qualifiers));
				if (garnucheAppliUtilisateur == null) {
					return "Vous n'êtes pas autorisé(e) à utiliser cette application.";
				}
				else {
					NSTimestamp now = DateCtrl.now();
					if (garnucheAppliUtilisateur.autiDateDebut().after(now)) {
						return "Vous n'êtes pas autorisé(e) à utiliser cette application avant le "
								+ DateCtrl.dateToString(garnucheAppliUtilisateur.autiDateDebut()) + ".";
					}
					else {
						if (garnucheAppliUtilisateur.autiDateFin() != null && garnucheAppliUtilisateur.autiDateFin().before(now)) {
							return "Vous n'êtes plus autorisé(e) à utiliser cette application depuis le "
									+ DateCtrl.dateToString(garnucheAppliUtilisateur.autiDateFin()) + ".";
						}
					}
				}
				session.setGarnucheAppliUtilisateur(garnucheAppliUtilisateur);

				// recherche du garnuche.utilisateur
				EOKeyValueQualifier loginQual = new EOKeyValueQualifier(EOUtilisateur.UTI_USERNAME_KEY,
						EOQualifier.QualifierOperatorCaseInsensitiveLike, session.connectedUserInfo().login());
				EOUtilisateur utilisateur = EOUtilisateur.fetchByQualifier(session.defaultEditingContext(), loginQual);
				if (utilisateur == null) {
					return "Vous n'avez pas de droits définis pour utiliser cette application.";
				}
				session.setUtilisateur(utilisateur);

				Application.dicoSessionIDUtilisateur().takeValueForKey(garnucheAppliUtilisateur, session.sessionID());
			}
		}
		return null;
	}

	public CktlLoginResponder getNewLoginResponder(NSDictionary actionParams) {
		return new DefaultLoginResponder(actionParams);
	}

	public class DefaultLoginResponder implements CktlLoginResponder {
		private NSDictionary actionParams;

		public DefaultLoginResponder(NSDictionary actionParams) {
			this.actionParams = actionParams;
		}

		public NSDictionary actionParams() {
			return actionParams;
		}

		public WOComponent loginAccepted(CktlLogin loginComponent) {
			return ((Session) context().session()).getSavedPageWithName(Accueil.class.getName());
		}

		public boolean acceptLoginName(String loginName) {
			return cktlApp.acceptLoginName(loginName);
		}

		public boolean acceptEmptyPassword() {
			return cktlApp.config().booleanForKey("ACCEPT_EMPTY_PASSWORD");
		}

		public String getRootPassword() {
			return cktlApp.getRootPassword();
		}
	}

}

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
package org.cocktail.inscription.serveur.components;

import org.cocktail.fwkcktlajaxwebext.serveur.CktlAjaxWOComponent;
import org.cocktail.fwkcktlwebapp.common.util.StringCtrl;
import org.cocktail.fwkcktlwebapp.server.components.CktlLoginResponder;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;

import er.extensions.appserver.ERXResponseRewriter;

public class LoginAdministratif extends CktlAjaxWOComponent {

	/** Le gestionnaire des evenements de composant de login local. */
	private CktlLoginResponder loginResponder;

	private String login;
	private String password;
	private String messageErreur;

	private String cRne;

	public LoginAdministratif(WOContext context) {
		super(context);
	}

	@Override
	public void appendToResponse(WOResponse response, WOContext context) {
		super.appendToResponse(response, context);
		ERXResponseRewriter.addScriptResourceInHead(response, context, "ScolarixModulesFwk.framework", "scripts/strings.js");
		ERXResponseRewriter.addScriptResourceInHead(response, context, "ScolarixModulesFwk.framework", "scripts/formatteurs.js");

		ERXResponseRewriter.addStylesheetResourceInHead(response, context, null, "styles/inscription.css");
	}

	/**
	 * Retourne la reference vers une instance de gestionnaire des evenements du composant de login local.
	 */
	public CktlLoginResponder loginResponder() {
		return loginResponder;
	}

	/**
	 * Definit le gestionnaire des evenements du composant de login local.
	 */
	public void registerLoginResponder(CktlLoginResponder responder) {
		loginResponder = responder;
	}

	/**
	 * @return the login
	 */
	public String login() {
		return login;
	}

	/**
	 * @param login
	 *            the login to set
	 */
	public void setLogin(String login) {
		this.login = login;
	}

	/**
	 * @return the password
	 */
	public String password() {
		return password;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the messageErreur
	 */
	public String messageErreur() {
		return messageErreur;
	}

	/**
	 * @param messageErreur
	 *            the messageErreur to set
	 */
	public void setMessageErreur(String messageErreur) {
		this.messageErreur = messageErreur;
	}

	public boolean isAfficherMessageErreur() {
		boolean isAfficherMessageErreur = false;

		isAfficherMessageErreur = !StringCtrl.isEmpty(messageErreur());

		return isAfficherMessageErreur;
	}

	public String cRne() {
		return cRne;
	}

	public void setCRne(String cRne) {
		this.cRne = cRne;
	}
}
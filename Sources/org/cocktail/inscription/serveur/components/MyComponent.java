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
import org.cocktail.inscription.serveur.Application;
import org.cocktail.inscription.serveur.Session;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;

import er.ajax.AjaxUtils;
import er.extensions.appserver.ERXWOContext;

public class MyComponent extends CktlAjaxWOComponent {

	public Application application = (Application) application();
	public Session session = (Session) super.session();

	private String containerId = null;

	private String onloadJS;

	public MyComponent(WOContext context) {
		super(context);
	}

	@Override
	public void appendToResponse(WOResponse response, WOContext context) {
		super.appendToResponse(response, context);
		AjaxUtils.addScriptResourceInHead(context, response, "controls.js");
	}

	public String getContainerId() {
		if (containerId == null) {
			containerId = ERXWOContext.safeIdentifierName(context(), true);
		}
		return containerId;
	}

	public Session session() {
		return session;
	}

	public String onloadJS() {
		if (onloadJS == null) {
			onloadJS = "";
		}
		return onloadJS;
	}

	public void setOnloadJS(String onloadJS) {
		this.onloadJS = onloadJS;
	}
}

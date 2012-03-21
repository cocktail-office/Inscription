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

import org.cocktail.fwkcktlwebapp.common.util.StringCtrl;
import org.cocktail.inscription.serveur.Application;
import org.cocktail.inscription.serveur.VersionMe;
import org.cocktail.scolaritefwk.serveur.metier.eos.EOScolFormationAnnee;
import org.cocktail.scolarix.serveur.components.DossierAdministratif;
import org.cocktail.scolarix.serveur.metier.eos.EOVEtablissementScolarite;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;

import er.ajax.CktlERXResponseRewriter;

public class Wrapper extends MyComponent {
	private String titre;
	private EOVEtablissementScolarite unEtablissement;
	private NSArray<EOVEtablissementScolarite> listeEtablissements = null;
	private EOScolFormationAnnee uneAnneeScolaire;
	private NSArray<EOScolFormationAnnee> listeAnneeScolaire = null;

	public Wrapper(WOContext context) {
		super(context);
	}

	@Override
	public void appendToResponse(WOResponse response, WOContext context) {
		super.appendToResponse(response, context);
		CktlERXResponseRewriter.addScriptResourceInHead(response, context, "ScolarixModulesFwk.framework", "scripts/strings.js");
		CktlERXResponseRewriter.addScriptResourceInHead(response, context, "ScolarixModulesFwk.framework", "scripts/formatteurs.js");
		CktlERXResponseRewriter.addScriptResourceInHead(response, context, "ScolarixModulesFwk.framework", "scripts/pwc-os.js");
		CktlERXResponseRewriter.addScriptResourceInHead(response, context, "ScolarixModulesFwk.framework", "scripts/scolarixmodules.js");

		CktlERXResponseRewriter.addStylesheetResourceInHead(response, context, "FwkCktlThemes", "css/CktlCommon.css");
		CktlERXResponseRewriter.addStylesheetResourceInHead(response, context, "FwkCktlThemes", "css/CktlCommonBleu.css");
		CktlERXResponseRewriter.addStylesheetResourceInHead(response, context, null, "styles/inscription.css");
		CktlERXResponseRewriter.addStylesheetResourceInHead(response, context, null, "styles/modules.css");
	}

	public WOComponent accueil() {
		if (DossierAdministratif.editing()) {
			return null;
		}
		session().defaultEditingContext().revert();
		session().defaultEditingContext().invalidateAllObjects();
		session().removeObjectForKey("MessageErreur");
		session().setErreur(null);
		Accueil page = (Accueil) session().getSavedPageWithName(Accueil.class.getName());
		page.setOnloadJS(null);
		return page;
	}

	public boolean canClick() {
		return !DossierAdministratif.editing();
	}

	public Erreur pageErreur() {
		Erreur nextPage = (Erreur) pageWithName(Erreur.class.getName());
		return nextPage;
	}

	public Telechargements pageTelechargements() {
		return (Telechargements) pageWithName(Telechargements.class.getName());
	}

	public String erreurScript() {
		String erreurScript = null;
		String messageErreur = (String) session().objectForKey("MessageErreur");
		if (StringCtrl.isEmpty(messageErreur)) {
			messageErreur = session().erreur();
			session().setErreur(null);
		}
		if (!StringCtrl.isEmpty(messageErreur)) {
			erreurScript = "alert('" + messageErreur + "');";
		}
		return erreurScript;
	}

	public String titre() {
		if (titre == null) {
			if (hasSession() && (session()).garnucheApplication() != null) {
				titre = (session()).garnucheApplication().applNom();
			}
			else {
				titre = (String) valueForBinding("titre");
			}
			if (titre == null) {
				titre = "INSCRIPTION";
			}
		}
		return titre;
	}

	public void setTitre(String titre) {
		this.titre = titre;
	}

	public String copyright() {
		return cktlApp().copyright();
	}

	public String contact() {
		return "mailto:" + ((Application) application()).config().stringForKey("ADMIN_MAIL") + "?subject=[INSCRIPTION] : question...";
	}

	public String version() {
		return VersionMe.htmlAppliVersion();
	}

	public boolean isNotPlusieursEtablissements() {
		return (listeEtablissements() == null || listeEtablissements().count() < 2);
	}

	public NSArray<EOVEtablissementScolarite> listeEtablissements() {
		if (listeEtablissements == null) {
			listeEtablissements = EOVEtablissementScolarite.fetchAll(edc());
		}
		return listeEtablissements;
	}

	public void setListeEtablissements(NSArray<EOVEtablissementScolarite> listeEtablissements) {
		this.listeEtablissements = listeEtablissements;
	}

	public NSArray<EOScolFormationAnnee> listeAnneeScolaire() {
		if (listeAnneeScolaire == null) {
			listeAnneeScolaire = EOScolFormationAnnee.fetchAll(
					edc(),
					new NSArray<EOSortOrdering>(EOSortOrdering.sortOrderingWithKey(EOScolFormationAnnee.FANN_DEBUT_KEY,
							EOSortOrdering.CompareAscending)));
		}
		return listeAnneeScolaire;
	}

	public void setListeAnneeScolaire(NSArray<EOScolFormationAnnee> listeAnneeScolaire) {
		this.listeAnneeScolaire = listeAnneeScolaire;
	}

	public EOVEtablissementScolarite unEtablissement() {
		return unEtablissement;
	}

	public void setUnEtablissement(EOVEtablissementScolarite unEtablissement) {
		this.unEtablissement = unEtablissement;
	}

	public EOScolFormationAnnee uneAnneeScolaire() {
		return uneAnneeScolaire;
	}

	public void setUneAnneeScolaire(EOScolFormationAnnee uneAnneeScolaire) {
		this.uneAnneeScolaire = uneAnneeScolaire;
	}
}
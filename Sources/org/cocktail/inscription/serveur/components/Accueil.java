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

import java.math.BigDecimal;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.foundation.NSMutableDictionary;

import er.extensions.appserver.ERXRedirect;

public class Accueil extends MyComponent {

	public BigDecimal etudNumero;

	public Accueil(WOContext context) {
		super(context);
	}

	public String commentaires() {
		return "On peut mettre des rollers à un cheval.<br>Il va beaucoup plus vite, mais beaucoup moins longtemps...";
	}

	public void reset() {
		session().setErreur(null);
	}

	public NouveauDossier saisirNouveauDossier() {
		reset();
		return (NouveauDossier) pageWithName(NouveauDossier.class.getName());
	}

	public WOComponent saisirNouveauDossierReInscription() {
		reset();
		return pageWithName(NouveauDossierReInscription.class.getName());
	}

	public WOComponent saisirNouveauDossierPreEtudiant() {
		reset();
		return pageWithName(NouveauDossierPreEtudiant.class.getName());
	}

	public WOComponent rechercheDossier() {
		reset();
		RechercheDossier page;
		try {
			page = (RechercheDossier) session().getSavedPageWithName(RechercheDossier.class.getName());
		}
		catch (Exception e) {
			page = (RechercheDossier) pageWithName(RechercheDossier.class.getName());
		}
		page.retourListeEtudiants();
		return page;
	}

	public WOActionResults rechercheDossierEtudNumero() {
		reset();
		RechercheDossier page = (RechercheDossier) pageWithName(RechercheDossier.class.getName());
		page.setQbe(new NSMutableDictionary<String, Object>(etudNumero, "etudNumero"));
		page.ctrl.rechercherLesEtudiants();
		ERXRedirect redirect = new ERXRedirect(context());
		redirect.setComponent(page);
		return redirect;
	}

	public WOComponent recherchePreDossier() {
		reset();
		RecherchePreDossier page;
		try {
			page = (RecherchePreDossier) session().getSavedPageWithName(RecherchePreDossier.class.getName());
		}
		catch (Exception e) {
			page = (RecherchePreDossier) pageWithName(RecherchePreDossier.class.getName());
		}
		page.retourListe();
		return page;
	}

	// TODO
	public WOComponent outils() {
		reset();
		return null;
	}

	// TODO
	public WOComponent options() {
		reset();
		return null;
	}

	public WOResponse imprimerLeDossier() {
		reset();
		// if (session().ctrlImpression().isPossibleImprimerLeDossierAdministratif()) {
		// return session().ctrlImpression().imprimerLeDossierDePreInscription();
		// }
		return null;
	}

	public WOComponent quitter() {
		reset();
		// if (session() != null && session().etudiant() != null && session().etudiant().numero() != null) {
		// Application.dicoSessionIDNumeroEtudiant().removeObjectForKey(String.valueOf(session().etudiant().numero().intValue()));
		// }
		return session().logout();
	}
}
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
package org.cocktail.inscription.serveur.controleurs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Enumeration;

import org.cocktail.inscription.serveur.components.NouveauDossierPreEtudiant;
import org.cocktail.inscription.serveur.components.RecherchePreDossier;
import org.cocktail.inscription.serveur.components.exceptions.CtrlInscriptionException;
import org.cocktail.scolarix.serveur.exception.EtudiantException;
import org.cocktail.scolarix.serveur.finder.FinderPreEtudiant;
import org.cocktail.scolarix.serveur.interfaces.IEtudiant;
import org.cocktail.scolarix.serveur.metier.eos.EOEtudiant;
import org.cocktail.scolarix.serveur.metier.eos.EOHistorique;
import org.cocktail.scolarix.serveur.metier.eos.EOPreEtudiant;
import org.cocktail.scolarix.serveur.metier.eos.EOPreIndividu;
import org.cocktail.scolarix.serveur.metier.eos.EORne;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestamp;

import er.extensions.appserver.ERXRedirect;

public class CtrlRecherchePreDossier {
	private RecherchePreDossier component = null;
	private EOEditingContext edc = null;

	private NSArray<EOPreEtudiant> preEtudiants = null;
	public EOPreEtudiant unPreEtudiant;

	private IEtudiant leEtudiant;
	private EOHistorique leDossier;

	public CtrlRecherchePreDossier(RecherchePreDossier component) {
		super();
		this.component = component;
		if (component != null) {
			edc = component.edc();
		}
	}

	public WOActionResults rechercherLesPreEtudiants() {
		setLeEtudiant(null);
		try {
			// if (component.qbe().allKeys().count() > 0) {
			EOSortOrdering nomPatronymiqueOrdering = EOSortOrdering.sortOrderingWithKey(EOPreEtudiant.TO_PRE_INDIVIDU_KEY + "."
					+ EOPreIndividu.NOM_PATRONYMIQUE_KEY, EOSortOrdering.CompareCaseInsensitiveAscending);
			EOSortOrdering prenomOrdering = EOSortOrdering.sortOrderingWithKey(EOPreEtudiant.TO_PRE_INDIVIDU_KEY + "." + EOPreIndividu.PRENOM_KEY,
					EOSortOrdering.CompareCaseInsensitiveAscending);
			NSArray<EOSortOrdering> sortOrderings = new NSArray<EOSortOrdering>(new EOSortOrdering[] { nomPatronymiqueOrdering, prenomOrdering });
			BigDecimal etudNumeroBG = (BigDecimal) component.qbe().valueForKey("etudNumero");
			preEtudiants = FinderPreEtudiant.getPreEtudiants(edc, etudNumeroBG == null ? null : new Integer(etudNumeroBG.intValue()),
					(String) component.qbe().valueForKey("etudCodeIne"), (String) component.qbe().valueForKey("nomPatronymique"), (String) component
							.qbe().valueForKey("prenom"), (NSTimestamp) component.qbe().valueForKey("dNaissance"), sortOrderings);
			if (preEtudiants == null || preEtudiants.count() == 0) {
				component.session().addSimpleInfoMessage("Pfff...", "Aucun pré-étudiant trouvé...");
			}
			// }
		}
		catch (CtrlInscriptionException e) {
			component.session().addSimpleErrorMessage("Erreur", e.getMessage());
		}
		return null;
	}

	public WOActionResults annulerLaRecherche() {
		reset();
		component.qbe().removeAllObjects();
		return null;
	}

	public void reset() {
		leEtudiant = null;
		leDossier = null;
		unPreEtudiant = null;
		preEtudiants = null;
	}

	public WOActionResults inscrire() {
		ERXRedirect redirectPage = null;
		NouveauDossierPreEtudiant page = (NouveauDossierPreEtudiant) component.pageWithName(NouveauDossierPreEtudiant.class.getName());
		try {
			page.ctrl.initDossier(unPreEtudiant());
			redirectPage = new ERXRedirect(component.context());
			redirectPage.setComponent(page);
			component.session().setErreur(null);
		}
		catch (CtrlInscriptionException e) {
			component.session().defaultEditingContext().revert();
			component.session().defaultEditingContext().invalidateAllObjects();
			WOResponse response = new WOResponse();
			response.setStatus(500);
			component.session().setErreur(e.getMessageJS());
			return response;
		}
		return redirectPage;
	}

	public WOComponent afficherLeDossier() {

		if (unPreEtudiant() != null) {
			EOEditingContext edc = component.session().defaultEditingContext();

			EORne rne = unPreEtudiant().rne();
			if (rne == null) {
				component.session().addSimpleErrorMessage("Erreur", "Impossible de déterminer l'établissement du pré-étudiant !");
				return null;
			}

			IEtudiant etudiant = null;
			try {
				edc.revert();
				edc.invalidateAllObjects();
				EOPreEtudiant preEtudiant = EOPreEtudiant.fetchByKeyValue(edc, EOPreEtudiant.ETUD_NUMERO_KEY, unPreEtudiant().etudNumero());
				Method m = component
						.session()
						.finder(rne.cRne())
						.getDeclaredMethod(
								"getEtudiantPreEtudiant",
								new Class[] { EOEditingContext.class, EOPreEtudiant.class, String.class,
										component.session().interfaceEtudiant(rne.cRne()) });
				etudiant = (IEtudiant) m.invoke(null, new Object[] { edc, preEtudiant, rne.cRne(), null });
			}
			catch (SecurityException e1) {
				e1.printStackTrace();
			}
			catch (NoSuchMethodException e2) {
				e2.printStackTrace();
			}
			catch (IllegalArgumentException e3) {
				e3.printStackTrace();
			}
			catch (IllegalAccessException e4) {
				e4.printStackTrace();
			}
			catch (EtudiantException e6) {
				e6.printStackTrace();
			}
			catch (InvocationTargetException e5) {
				if (e5.getCause().getClass().equals(EtudiantException.class)) {
					EtudiantException exception = (EtudiantException) e5.getCause();
					component.session().addSimpleErrorMessage("Erreur", exception.getMessage());
					return null;
				}
			}

			if (etudiant != null) {
				EOHistorique historique = etudiant.historiquePlusRecent(new Integer(3000));
				etudiant.setAnneeInscriptionEnCours(historique.histAnneeScol());
				etudiant.setEtudType(EOEtudiant.ETUD_TYPE_INSCRIPTION);
				etudiant.setRne(rne);

				setLeEtudiant(etudiant);
				setLeDossier(historique);

				NSArray<EtudiantException> userInfos = etudiant.userInfos();
				if (userInfos != null && userInfos.count() > 0) {
					String messages = "";
					Enumeration<EtudiantException> enumUserInfos = userInfos.objectEnumerator();
					while (enumUserInfos.hasMoreElements()) {
						EtudiantException exception = enumUserInfos.nextElement();
						messages += exception.getMessageJS() + "\\n";
					}
					component.session().addSimpleErrorMessage("Erreur", messages);
				}
			}
		}

		return null;
	}

	public NSArray<EOPreEtudiant> preEtudiants() {
		return preEtudiants;
	}

	public void setPreEtudiants(NSArray<EOPreEtudiant> preEtudiants) {
		this.preEtudiants = preEtudiants;
	}

	public EOPreEtudiant unPreEtudiant() {
		return unPreEtudiant;
	}

	public void setUnPreEtudiant(EOPreEtudiant unPreEtudiant) {
		this.unPreEtudiant = unPreEtudiant;
	}

	public IEtudiant leEtudiant() {
		return leEtudiant;
	}

	public void setLeEtudiant(IEtudiant leEtudiant) {
		this.leEtudiant = leEtudiant;
	}

	public EOHistorique leDossier() {
		return leDossier;
	}

	public void setLeDossier(EOHistorique leDossier) {
		this.leDossier = leDossier;
	}

}

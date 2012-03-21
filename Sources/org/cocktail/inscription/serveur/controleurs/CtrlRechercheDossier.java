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

import org.cocktail.fwkcktlpersonne.common.metier.EOIndividu;
import org.cocktail.inscription.serveur.components.NouveauDossierReInscription;
import org.cocktail.inscription.serveur.components.RechercheDossier;
import org.cocktail.inscription.serveur.components.exceptions.CtrlInscriptionException;
import org.cocktail.scolarix.serveur.exception.EtudiantException;
import org.cocktail.scolarix.serveur.finder.FinderEtudiant;
import org.cocktail.scolarix.serveur.interfaces.IEtudiant;
import org.cocktail.scolarix.serveur.metier.eos.EOEtudiant;
import org.cocktail.scolarix.serveur.metier.eos.EOHistorique;
import org.cocktail.scolarix.serveur.metier.eos.EORne;
import org.cocktail.scolarix.serveur.metier.eos.EOVEtablissementScolarite;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSTimestamp;

import er.extensions.appserver.ERXRedirect;

public class CtrlRechercheDossier {
	private RechercheDossier component = null;
	private EOEditingContext edc = null;

	private NSArray<EOEtudiant> etudiants = null;
	private IEtudiant unEtudiant;
	private IEtudiant leEtudiant;

	private NSArray<EOHistorique> dossiers = null;
	private EOHistorique unDossier;
	private EOHistorique leDossier;
	private EOVEtablissementScolarite unEtablissement;
	private NSArray<EOVEtablissementScolarite> listeEtablissements = null;

	public CtrlRechercheDossier(RechercheDossier component) {
		super();
		this.component = component;
		if (component != null) {
			edc = component.edc();
		}
	}

	public WOActionResults rechercherLesEtudiants() {
		setLeEtudiant(null);
		setLeDossier(null);
		try {
			if (component.qbe().allKeys().count() > 0) {
				if (component.qbe().allKeys().count() == 1 && component.qbe().valueForKey("etablissement") != null) {
					return null;
				}
				EOSortOrdering nomPatronymiqueOrdering = EOSortOrdering.sortOrderingWithKey(EOEtudiant.TO_FWKPERS__INDIVIDU_KEY + "."
						+ EOIndividu.NOM_PATRONYMIQUE_KEY, EOSortOrdering.CompareCaseInsensitiveAscending);
				EOSortOrdering prenomOrdering = EOSortOrdering.sortOrderingWithKey(EOEtudiant.TO_FWKPERS__INDIVIDU_KEY + "." + EOIndividu.PRENOM_KEY,
						EOSortOrdering.CompareCaseInsensitiveAscending);
				NSArray<EOSortOrdering> sortOrderings = new NSArray<EOSortOrdering>(new EOSortOrdering[] { nomPatronymiqueOrdering, prenomOrdering });
				BigDecimal anneeBG = (BigDecimal) component.qbe().valueForKey("annee");
				BigDecimal etudNumeroBG = (BigDecimal) component.qbe().valueForKey("etudNumero");
				NSArray<EOEtudiant> etudiants = null;
				if (etudNumeroBG != null) {
					EOEtudiant e = (EOEtudiant) FinderEtudiant.getEtudiant(edc, new Integer(etudNumeroBG.intValue()), null, null);
					if (e != null) {
						etudiants = new NSArray<EOEtudiant>(e);
					}
				}
				else {
					etudiants = FinderEtudiant.getEtudiants(edc, anneeBG == null ? null : new Integer(anneeBG.intValue()),
							etudNumeroBG == null ? null : new Integer(etudNumeroBG.intValue()), (String) component.qbe().valueForKey("etudCodeIne"),
							(String) component.qbe().valueForKey("nomPatronymique"), (String) component.qbe().valueForKey("prenom"),
							(NSTimestamp) component.qbe().valueForKey("dNaissance"),
							(EOVEtablissementScolarite) component.qbe().valueForKey("etablissement"), sortOrderings);
				}
				if (etudiants == null || etudiants.count() == 0) {
					component.session().addSimpleInfoMessage("Pfff...", "Aucun étudiant trouvé...");
				}
				setEtudiants(etudiants);
				if (etudiants != null && etudiants.count() == 1) {
					setUnEtudiant(etudiants.lastObject());
					return afficherLesDossiers();
				}
			}
		}
		catch (CtrlInscriptionException e) {
			component.session().addSimpleErrorMessage("Erreur", e.getMessage());
			return null;
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
		unEtudiant = null;
		etudiants = null;
		leDossier = null;
		unDossier = null;
		dossiers = null;
	}

	public WOActionResults reInscrire() {
		ERXRedirect redirectPage = null;
		NouveauDossierReInscription page = (NouveauDossierReInscription) component.pageWithName(NouveauDossierReInscription.class.getName());
		try {
			component.session().setErreur(null);
			page.ctrl.initDossier(unEtudiant());
			redirectPage = new ERXRedirect(component.context());
			redirectPage.setComponent(page);
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

	public WOComponent afficherLesDossiers() {
		if (unEtudiant() != null) {
			EOEtudiant unEtudiant = (EOEtudiant) unEtudiant();

			setLeEtudiant(unEtudiant);
			if (unEtudiant != null) {
				NSArray<EtudiantException> userInfos = unEtudiant.userInfos();
				if (userInfos != null && userInfos.count() > 0) {
					String messages = "";
					Enumeration<EtudiantException> enumUserInfos = userInfos.objectEnumerator();
					while (enumUserInfos.hasMoreElements()) {
						EtudiantException exception = enumUserInfos.nextElement();
						messages += exception.getMessageJS() + "\\n";
					}
					component.session().addSimpleErrorMessage("Erreur", messages);
				}
				setDossiers(unEtudiant.toHistoriques(
						null,
						new NSArray<EOSortOrdering>(EOSortOrdering.sortOrderingWithKey(EOHistorique.HIST_ANNEE_SCOL_KEY,
								EOSortOrdering.CompareDescending))));
				if (dossiers() != null && dossiers().count() == 1) {
					setUnDossier(dossiers().lastObject());
					return afficherLeDossier();
				}
			}
		}
		return null;
	}

	public WOComponent afficherLeDossier() {
		if (unEtudiant() != null && unDossier() != null) {

			EORne rne = unEtudiant().rne();
			if (rne == null) {
				component.session().addSimpleErrorMessage("Erreur", "Impossible de déterminer l'établissement de l'étudiant !");
				return null;
			}

			IEtudiant etudiant = null;
			try {
				edc.revert();
				edc.invalidateAllObjects();
				Method m = component.session().finder(rne.cRne())
						.getDeclaredMethod("getEtudiant", new Class[] { EOEditingContext.class, Integer.class, NSTimestamp.class, Integer.class });
				etudiant = (IEtudiant) m
						.invoke(null, new Object[] { component.session().defaultEditingContext(), unEtudiant().numero(), null, null });
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
				e5.printStackTrace();
				if (e5.getCause().getClass().equals(EtudiantException.class)) {
					EtudiantException exception = (EtudiantException) e5.getCause();
					component.session().addSimpleErrorMessage("Erreur", exception.getMessage());
				}
			}

			if (etudiant != null) {
				etudiant.setAnneeInscriptionEnCours(unDossier().histAnneeScol());
				etudiant.setEtudType(EOEtudiant.ETUD_TYPE_INSCRIPTION);
				etudiant.setRne(rne);

				setLeEtudiant(etudiant);
				setLeDossier(unDossier());

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

	public NSArray<EOVEtablissementScolarite> listeEtablissements() {
		if (listeEtablissements == null) {
			listeEtablissements = EOVEtablissementScolarite.fetchAll(edc);
		}
		return listeEtablissements;
	}

	public NSArray<EOEtudiant> etudiants() {
		return etudiants;
	}

	public void setEtudiants(NSArray<EOEtudiant> etudiants) {
		this.etudiants = etudiants;
	}

	public IEtudiant unEtudiant() {
		return unEtudiant;
	}

	public void setUnEtudiant(IEtudiant unEtudiant) {
		this.unEtudiant = unEtudiant;
	}

	public IEtudiant leEtudiant() {
		return leEtudiant;
	}

	public void setLeEtudiant(IEtudiant leEtudiant) {
		this.leEtudiant = leEtudiant;
	}

	public NSArray<EOHistorique> dossiers() {
		return dossiers;
	}

	public void setDossiers(NSArray<EOHistorique> dossiers) {
		this.dossiers = dossiers;
	}

	public EOHistorique unDossier() {
		return unDossier;
	}

	public void setUnDossier(EOHistorique unDossier) {
		this.unDossier = unDossier;
	}

	public EOHistorique leDossier() {
		return leDossier;
	}

	public void setLeDossier(EOHistorique leDossier) {
		this.leDossier = leDossier;
	}

	public EOVEtablissementScolarite unEtablissement() {
		return unEtablissement;
	}

	public void setUnEtablissement(EOVEtablissementScolarite unEtablissement) {
		this.unEtablissement = unEtablissement;
	}

}

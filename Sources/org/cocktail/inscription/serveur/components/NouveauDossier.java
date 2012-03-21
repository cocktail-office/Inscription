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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.cocktail.fwkcktlpersonne.common.metier.EOIndividu;
import org.cocktail.inscription.serveur.components.exceptions.CtrlInscriptionException;
import org.cocktail.inscription.serveur.controleurs.CtrlNouveauDossier;
import org.cocktail.scolarix.serveur.exception.EtudiantException;
import org.cocktail.scolarix.serveur.interfaces.IEtudiant;
import org.cocktail.scolarix.serveur.metier.eos.EOEtudiant;
import org.cocktail.scolarix.serveur.metier.eos.EOHistorique;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.eocontrol.EOEditingContext;

import er.extensions.appserver.ERXRedirect;

public class NouveauDossier extends MyComponent {
	public CtrlNouveauDossier ctrl = null;

	private IEtudiant etudiant = null;
	private EOHistorique historique = null;

	public NouveauDossier(WOContext context) {
		super(context);
		ctrl = new CtrlNouveauDossier(this);
	}

	public WOActionResults creer() {
		saisirNouveauDossier(null);
		return null;
	}

	public WOActionResults selectionner() {
		if (ctrl == null || ctrl.getSelectedPersonne() == null) {
			return null;
		}
		EOIndividu individu = (EOIndividu) ctrl.getSelectedPersonne();
		if (individu == null) {
			return null;
		}
		IEtudiant etudiant = EOEtudiant.fetchByKeyValue(ctrl.getEdc(), EOEtudiant.NO_INDIVIDU_KEY, individu.noIndividu());
		if (etudiant != null) {
			return reInscrire(etudiant);
		}
		saisirNouveauDossier(individu);
		return null;
	}

	private WOActionResults reInscrire(IEtudiant etudiant) {
		ERXRedirect redirectPage = null;
		NouveauDossierReInscription page = (NouveauDossierReInscription) pageWithName(NouveauDossierReInscription.class.getName());
		try {
			page.ctrl.initDossier(etudiant);
			redirectPage = new ERXRedirect(context());
			redirectPage.setComponent(page);
		}
		catch (CtrlInscriptionException e) {
			session().defaultEditingContext().revert();
			session().defaultEditingContext().invalidateAllObjects();
			session().addSimpleErrorMessage("Erreur", e.getMessage());
			return null;
		}
		return redirectPage;
	}

	private void saisirNouveauDossier(EOIndividu individu) {
		IEtudiant etudiant = null;
		EOEditingContext edc = ctrl.getEdc();
		try {
			edc.revert();
			edc.invalidateAllObjects();
			Method m = session().factory().getDeclaredMethod("createForInscription",
					new Class[] { EOEditingContext.class, Integer.class, String.class, EOIndividu.class });
			etudiant = (IEtudiant) m.invoke(null, new Object[] { edc, session().anneeScolaire().fannDebut(), session().garnucheApplication().cRne(), individu });
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
		catch (InvocationTargetException e5) {
			e5.printStackTrace();
		}
		catch (EtudiantException e6) {
			e6.printStackTrace();
		}
		setEtudiant(etudiant);
		setHistorique(etudiant.historique(session().anneeScolaire().fannDebut()));
	}

	public IEtudiant etudiant() {
		return etudiant;
	}

	public void setEtudiant(IEtudiant etudiant) {
		this.etudiant = etudiant;
	}

	public EOHistorique historique() {
		return historique;
	}

	public void setHistorique(EOHistorique historique) {
		this.historique = historique;
	}

	public boolean isAfficherDossierEtudiant() {
		return etudiant != null;
	}

}
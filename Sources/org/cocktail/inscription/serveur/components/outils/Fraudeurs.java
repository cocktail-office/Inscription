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
package org.cocktail.inscription.serveur.components.outils;

import org.cocktail.fwkcktlpersonne.common.metier.IPersonne;
import org.cocktail.fwkcktlwebapp.common.util.DateCtrl;
import org.cocktail.inscription.serveur.components.MyComponent;
import org.cocktail.scolarix.serveur.exception.ScolarixFwkException;
import org.cocktail.scolarix.serveur.metier.eos.EOEtudiant;
import org.cocktail.scolarix.serveur.metier.eos.EOFraudeur;
import org.cocktail.scolarix.serveur.metier.eos.EOHistorique;
import org.cocktail.scolarix.serveur.metier.eos.EOInscDipl;
import org.cocktail.scolarix.serveur.process.ProcessDelFraudeur;
import org.cocktail.scolarix.serveur.process.ProcessInsFraudeur;
import org.cocktail.scolarix.serveur.process.ProcessUpdFraudeur;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WODisplayGroup;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

public class Fraudeurs extends MyComponent {

	private final DgFraudeurListDelegate dgFraudeurListDelegate = new DgFraudeurListDelegate();
	private WODisplayGroup dgFraudeursList = new WODisplayGroup();
	private EOFraudeur selectedFraudeur, fraudeurEnCours;
	private EOInscDipl uneInscription;

	public Fraudeurs(WOContext context) {
		super(context);
		dgFraudeursList().setDelegate(dgFraudeurListDelegate);
		init();
	}

	private void init() {
		NSMutableArray<EOSortOrdering> sort = new NSMutableArray<EOSortOrdering>();
		sort.addObject(new EOSortOrdering(EOFraudeur.FRAU_NOM_KEY, EOSortOrdering.CompareCaseInsensitiveAscending));
		sort.addObject(new EOSortOrdering(EOFraudeur.FRAU_PRENOM_KEY, EOSortOrdering.CompareCaseInsensitiveAscending));
		dgFraudeursList().setSortOrderings(sort);
		dgFraudeursList().setObjectArray(EOFraudeur.fetchAll(edc()));
	}

	private void update() {
		dgFraudeursList().setObjectArray(EOFraudeur.fetchAll(edc()));
	}

	public Integer persIdUtilisateur() {
		return new Integer(session.connectedUserInfo().persId().intValue());
	}

	public String legendFieldsetText() {
		if (fraudeurEnCours != null) {
			if (fraudeurEnCours.isNew()) {
				return "Fraudeurs - Ajout en cours...";
			}
			else {
				return "Fraudeurs - Modification en cours...";
			}
		}
		return "Fraudeurs";
	}

	public String etudiantDisplayString() {
		if (fraudeurEnCours != null && fraudeurEnCours.toEtudiant() != null) {
			return fraudeurEnCours.toEtudiant().etudNumero() + " - " + fraudeurEnCours.toEtudiant().toFwkpers_Individu().getNomPrenomAffichage();
		}
		return null;
	}

	public NSArray<EOInscDipl> listeInscriptions() {
		if (fraudeurEnCours != null && fraudeurEnCours.toEtudiant() != null) {
			NSArray<EOHistorique> historiques = fraudeurEnCours.toEtudiant().toHistoriques();
			if (historiques != null) {
				NSMutableArray<EOInscDipl> res = new NSMutableArray<EOInscDipl>();
				for (EOHistorique histo : historiques) {
					res.addObjectsFromArray(histo.toInscDipls());
				}
				return res;
			}
		}
		return null;
	}

	public WOActionResults ajouter() {
		EOFraudeur newOne = EOFraudeur.create(edc());
		newOne.setNew(true);
		newOne.setFrauDateDebut(DateCtrl.now());
		setFraudeurEnCours(newOne);
		return null;
	}

	public WOActionResults modifier() {
		getSelectedFraudeur().setNew(false);
		setFraudeurEnCours(getSelectedFraudeur());
		fraudeurEnCours.setOldNomBeforeUpdate(fraudeurEnCours.frauNom());
		fraudeurEnCours.setOldPrenomBeforeUpdate(fraudeurEnCours.frauPrenom());
		return null;
	}

	public WOActionResults supprimer() {
		try {
			if (getSelectedFraudeur() != null) {
				ProcessDelFraudeur.enregistrer(cktlSession().dataBus(), edc(), getSelectedFraudeur());
			}
		}
		catch (ScolarixFwkException sfe) {
			mySession().addSimpleErrorMessage("Erreur", sfe.getMessageFormatte());
		}
		update();
		return null;
	}

	public WOActionResults valider() {
		try {
			if (getFraudeurEnCours().isNew()) {
				ProcessInsFraudeur.enregistrer(cktlSession().dataBus(), edc(), getFraudeurEnCours());
			}
			else {
				ProcessUpdFraudeur.enregistrer(cktlSession().dataBus(), edc(), getFraudeurEnCours());
			}
		}
		catch (ScolarixFwkException sfe) {
			mySession().addSimpleErrorMessage("Erreur", sfe.getMessageFormatte());
			return null;
		}
		setFraudeurEnCours(null);
		update();
		return null;
	}

	public WOActionResults annuler() {
		edc().revert();
		setFraudeurEnCours(null);
		return null;
	}

	public WOActionResults supprimerEtudiant() {
		if (fraudeurEnCours != null && fraudeurEnCours.toEtudiant() != null) {
			fraudeurEnCours.setToEtudiantRelationship(null);
		}
		return null;
	}

	public String getFraudeursListId() {
		return getComponentId() + "_FraudeursListId";
	}

	public WODisplayGroup dgFraudeursList() {
		return dgFraudeursList;
	}

	public EOFraudeur getSelectedFraudeur() {
		return selectedFraudeur;
	}

	public void setSelectedFraudeur(EOFraudeur selectedFraudeur) {
		this.selectedFraudeur = selectedFraudeur;
	}

	public EOFraudeur getFraudeurEnCours() {
		return fraudeurEnCours;
	}

	public void setFraudeurEnCours(EOFraudeur fraudeurEnCours) {
		this.fraudeurEnCours = fraudeurEnCours;
	}

	public EOInscDipl uneInscription() {
		return uneInscription;
	}

	public void setUneInscription(EOInscDipl uneInscription) {
		this.uneInscription = uneInscription;
	}

	public IPersonne getSelectedPersonne() {
		if (fraudeurEnCours != null && fraudeurEnCours.toEtudiant() != null) {
			return fraudeurEnCours.toEtudiant().toFwkpers_Individu();
		}
		return null;
	}

	public void setSelectedPersonne(IPersonne selectedPersonne) {
		if (fraudeurEnCours != null) {
			fraudeurEnCours.setToEtudiantRelationship(EOEtudiant.fetchFirstByKeyValue(edc(), EOEtudiant.TO_FWKPERS__INDIVIDU_KEY, selectedPersonne));
			if (fraudeurEnCours.toEtudiant() != null) {
				fraudeurEnCours.setFrauNom(fraudeurEnCours.toEtudiant().toFwkpers_Individu().nomUsuel());
				fraudeurEnCours.setFrauPrenom(fraudeurEnCours.toEtudiant().toFwkpers_Individu().prenom());
				fraudeurEnCours.setFrauDateNais(fraudeurEnCours.toEtudiant().toFwkpers_Individu().dNaissance());
			}
		}
	}

	public class DgFraudeurListDelegate {
		public void displayGroupDidChangeSelection(WODisplayGroup group) {
			setSelectedFraudeur((EOFraudeur) group.selectedObject());
		}
	}
}
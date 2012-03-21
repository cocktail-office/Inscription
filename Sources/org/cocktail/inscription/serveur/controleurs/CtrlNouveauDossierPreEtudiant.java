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
import java.util.Enumeration;

import org.cocktail.inscription.serveur.components.NouveauDossierPreEtudiant;
import org.cocktail.inscription.serveur.components.exceptions.CtrlInscriptionException;
import org.cocktail.scolarix.serveur.exception.EtudiantException;
import org.cocktail.scolarix.serveur.exception.ScolarixFwkException;
import org.cocktail.scolarix.serveur.finder.FinderPreEtudiant;
import org.cocktail.scolarix.serveur.interfaces.IEtudiant;
import org.cocktail.scolarix.serveur.metier.eos.EOEtudiant;
import org.cocktail.scolarix.serveur.metier.eos.EOHistorique;
import org.cocktail.scolarix.serveur.metier.eos.EOPreEtudiant;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;

public class CtrlNouveauDossierPreEtudiant {
	private NouveauDossierPreEtudiant component = null;
	private EOEditingContext edc = null;

	private IEtudiant etudiant = null;
	private EOHistorique historique = null;

	public CtrlNouveauDossierPreEtudiant(NouveauDossierPreEtudiant component) {
		super();
		this.component = component;
		if (component != null) {
			edc = component.edc();
		}
	}

	public void rechercherPreEtudiantEtSaisirDossier() {
		try {
			if (component.qbe().allKeys().count() > 0) {
				String numeroINE = (String) component.qbe().valueForKey("numeroINE");
				if (numeroINE == null) {
					throw new CtrlInscriptionException("Mauvais numéro INE/BEA !");
				}

				EOPreEtudiant preEtudiant = FinderPreEtudiant.getPreEtudiantIne(edc, numeroINE, null);
				if (preEtudiant == null) {
					throw new CtrlInscriptionException("Aucun étudiant ne correspond à ce numéro INE/BEA !");
				}

				initDossier(preEtudiant);
			}
			else {
				throw new CtrlInscriptionException("Indiquez un numéro INE/BEA.");
			}
			component.session().setErreur(null);
		}
		catch (ScolarixFwkException e) {
			setEtudiant(null);
			component.session().defaultEditingContext().revert();
			component.session().defaultEditingContext().invalidateAllObjects();
			component.session().setErreur(e.getMessageJS());
		}
	}

	public void initDossier(EOPreEtudiant lePreEtudiant) throws CtrlInscriptionException {
		if (lePreEtudiant == null) {
			throw new CtrlInscriptionException("Aucun pré-étudiant !");
		}

		if (lePreEtudiant.rne() == null || lePreEtudiant.rne().cRne() == null) {
			throw new CtrlInscriptionException("Impossible de déterminer l'établissement d'inscription de ce pré-étudiant !");
		}
		// if (lePreEtudiant.rne().cRne().equalsIgnoreCase(component.session().codeRne()) == false) {
		// component.session().setCodeRne(lePreEtudiant.rne().cRne());
		// }

		Integer anneeInscription = component.session().anneeScolaire().fannDebut();
		if (anneeInscription == null) {
			throw new CtrlInscriptionException("Problème: impossible de déterminer l'année d'inscription en cours !");
		}

		IEtudiant tempEtudiant = EOEtudiant.fetchByKeyValue(edc, EOEtudiant.ETUD_CODE_INE_KEY, lePreEtudiant.etudCodeIne());
		if (tempEtudiant != null && tempEtudiant.historique(anneeInscription) != null) {
			throw new CtrlInscriptionException("Cet étudiant est déjà inscrit pour l'année " + anneeInscription);
		}

		try {
			edc.revert();
			edc.invalidateAllObjects();
			EOPreEtudiant preEtudiant = EOPreEtudiant.fetchByKeyValue(edc, EOPreEtudiant.ETUD_NUMERO_KEY, lePreEtudiant.etudNumero());
			Method m = component.session().finder(lePreEtudiant.rne().cRne()).getDeclaredMethod(
					"getEtudiantPreEtudiant",
					new Class[] { EOEditingContext.class, EOPreEtudiant.class, String.class,
							component.session().interfaceEtudiant(lePreEtudiant.rne().cRne()) });
			etudiant = (IEtudiant) m.invoke(null, new Object[] { edc, preEtudiant, lePreEtudiant.rne().cRne(), null });
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
				component.session().setErreur(exception.getMessageJS());
			}
			else {
				e5.printStackTrace();
			}
		}

		if (etudiant != null) {
			if (etudiant.historique(anneeInscription) == null) {
				throw new CtrlInscriptionException("Cette pré-inscription ne correspond pas à l'année d'inscription en cours (" + anneeInscription
						+ ")");
			}

			historique = etudiant.historique(anneeInscription);
			etudiant.setAnneeInscriptionEnCours(anneeInscription);
			if (lePreEtudiant.isPreInscription()) {
				etudiant.setEtudType(EOEtudiant.ETUD_TYPE_INSCRIPTION);
				etudiant.setNumero(null);
			}
			else {
				etudiant.setEtudType(EOEtudiant.ETUD_TYPE_RE_INSCRIPTION);
			}

			NSArray<EtudiantException> userInfos = etudiant.userInfos();
			if (userInfos != null && userInfos.count() > 0) {
				String messages = "";
				Enumeration<EtudiantException> enumUserInfos = userInfos.objectEnumerator();
				while (enumUserInfos.hasMoreElements()) {
					EtudiantException exception = enumUserInfos.nextElement();
					messages += exception.getMessageJS() + "\\n";
				}
				component.session().setObjectForKey(messages, "MessageErreur");
			}
		}
	}

	public boolean isAfficherQbe() {
		return etudiant() == null;
	}

	public boolean isAfficherDossier() {
		return etudiant() != null;
	}

	public void reset() {
		etudiant = null;
	}

	public IEtudiant etudiant() {
		if (etudiant == null) {
			etudiant = (EOEtudiant) component.valueForBinding("etudiant");
		}
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

}

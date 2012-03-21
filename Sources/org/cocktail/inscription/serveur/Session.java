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

import org.cocktail.fwkcktlajaxwebext.serveur.CocktailAjaxSession;
import org.cocktail.fwkcktlwebapp.common.CktlLog;
import org.cocktail.fwkcktlwebapp.common.util.DateCtrl;
import org.cocktail.fwkcktlwebapp.common.util.StringCtrl;
import org.cocktail.scolaritefwk.serveur.metier.eos.EOScolFormationAnnee;
import org.cocktail.scolarix.serveur.metier.eos.EOGarnucheAppliUtilisateur;
import org.cocktail.scolarix.serveur.metier.eos.EOGarnucheApplication;
import org.cocktail.scolarix.serveur.metier.eos.EOUtilisateur;
import org.cocktail.scolarix.serveur.metier.eos.EOVEtablissementScolarite;

import com.webobjects.foundation.NSMutableDictionary;

public class Session extends CocktailAjaxSession {
	private static final long serialVersionUID = 1L;

	// Message d'erreur a afficher via une alerte JS
	public String erreur = null;

	public EOGarnucheApplication garnucheApplication;
	public EOGarnucheAppliUtilisateur garnucheAppliUtilisateur;
	private EOUtilisateur utilisateur;

	private EOVEtablissementScolarite etablissement = null;
	private EOScolFormationAnnee anneeScolaire = null;

	public Session() {
		super();
		CktlLog.rawLog("[Session.java] " + DateCtrl.currentDateTimeString() + " - New " + sessionID() + "(" + timeOut() + ") ");
	}

	public Session(String sessionID) {
		super();
		CktlLog.rawLog("[Session.java] " + DateCtrl.currentDateTimeString() + " - New " + sessionID + "(" + timeOut() + ") ");
	}

	public void terminate() {
		NSMutableDictionary<EOGarnucheAppliUtilisateur, String> dicoSessionIDUtilisateur = Application.dicoSessionIDUtilisateur();
		if (dicoSessionIDUtilisateur != null && dicoSessionIDUtilisateur.containsValue(sessionID())) {
			dicoSessionIDUtilisateur.removeObjectForKey(sessionID());
		}
		CktlLog.rawLog("[Session.java] " + DateCtrl.currentDateTimeString() + " - terminate(" + sessionID() + ")");

		super.terminate();
	}

	public EOVEtablissementScolarite etablissement() {
		if (etablissement == null) {
			if (garnucheApplication() != null) {
				setEtablissement(garnucheApplication().toVEtablissementScolarite());
			}
		}
		return etablissement;
	}

	public void setEtablissement(EOVEtablissementScolarite etablissement) {
		this.etablissement = etablissement;
	}

	public EOScolFormationAnnee anneeScolaire() {
		if (anneeScolaire == null) {
			if (garnucheApplication() != null) {
				setAnneeScolaire(garnucheApplication().scolFormationAnneeEnCours());
			}
		}
		return anneeScolaire;
	}

	public void setAnneeScolaire(EOScolFormationAnnee anneeScolaire) {
		this.anneeScolaire = anneeScolaire;
	}

	public void setErreur(String erreur) {
		this.erreur = erreur;
	}

	public String erreur() {
		return erreur;
	}

	public EOGarnucheApplication garnucheApplication() {
		return garnucheApplication;
	}

	public void setGarnucheApplication(EOGarnucheApplication garnucheApp) {
		this.garnucheApplication = garnucheApp;
		if (garnucheApp != null) {
			if (anneeScolaire() == null) {
				setAnneeScolaire(garnucheApp.scolFormationAnneeEnCours());
			}
			if (etablissement() == null) {
				setEtablissement(garnucheApp.toVEtablissementScolarite());
			}
		}
	}

	/**
	 * Recherche d'une classe spécifique si elle existe, sinon renvoie la classe générale. On recherche du niveau le plus fin jusqu'au plus
	 * général, jusqu'à trouver une classe... Si aucune classe spécifique n'est trouvée, on renvoie la classe générale.<br>
	 * La logique de recherche dans l'ordre (stoppe dès qu'une classe est trouvée) :<br>
	 * - Si le paramètre SUFFIXE_SPECIFICITE est défini :<br>
	 * . Recherche d'une classe avec suffixeSpécificité + codePays + codeRne<br>
	 * . Si non trouvé, recherche d'une classe avec suffixeSpécificité + codePays<br>
	 * . Si non trouvé, recherche d'une classe avec suffixeSpécificité<br>
	 * - Si le paramètre SUFFIXE_SPECIFICITE n'est pas défini ou aucune classe trouvée avec :<br>
	 * . Recherche d'une classe avec codePays + codeRne<br>
	 * . Si non trouvé, recherche d'une classe avec codePays<br>
	 * . Si non trouvé, recherche d'une classe avec codeRne<br>
	 * . Si non trouvé, recherche de la classe générale (doit toujours exister, sinon l'application s'arrête)<br>
	 * 
	 * @param className
	 *            Le nom complet de la classe à chercher (package inclus)
	 * @param cRne
	 *            L'établissement pour lequel on recherche une spécificité éventuelle
	 * @return La classe qui va bien... Doit forcément retourner une classe, ou bien l'application s'arrête...
	 */
	private Class getGoodClass(String className, String cRne) {
		Class goodClass = null;
		String suffixeSpecificite = cktlApp.config().stringForKey("SUFFIXE_SPECIFICITE");
		String codePays = cktlApp.config().stringForKey("GRHUM_C_PAYS_DEFAUT");
		if (StringCtrl.isEmpty(suffixeSpecificite) == false) {
			if (cRne != null) {
				try {
					System.out.print("Looking for " + className + suffixeSpecificite + codePays + cRne + "... ");
					goodClass = Class.forName(className + suffixeSpecificite + codePays + cRne);
					System.out.println("YES!");
					return goodClass;
				}
				catch (ClassNotFoundException e1) {
					System.out.println("NO...");
				}
			}
			try {
				System.out.print("Looking for " + className + suffixeSpecificite + codePays + "... ");
				goodClass = Class.forName(className + suffixeSpecificite + codePays);
				System.out.println("YES!");
				return goodClass;
			}
			catch (ClassNotFoundException e2) {
				System.out.println("NO...");
			}
			try {
				System.out.print("Looking for " + className + suffixeSpecificite + "... ");
				goodClass = Class.forName(className + suffixeSpecificite);
				System.out.println("YES!");
				return goodClass;
			}
			catch (ClassNotFoundException e3) {
				System.out.println("NO...");
			}
		}
		if (cRne != null) {
			try {
				System.out.print("Looking for " + className + codePays + cRne + "... ");
				goodClass = Class.forName(className + codePays + cRne);
				System.out.println("YES!");
				return goodClass;
			}
			catch (ClassNotFoundException e4) {
				System.out.println("NO...");
			}
		}
		try {
			System.out.print("Looking for " + className + codePays + "... ");
			goodClass = Class.forName(className + codePays);
			System.out.println("YES!");
			return goodClass;
		}
		catch (ClassNotFoundException e5) {
			System.out.println("NO...");
		}
		if (cRne != null) {
			try {
				System.out.print("Looking for " + className + cRne + "... ");
				goodClass = Class.forName(className + cRne);
				System.out.println("YES!");
				return goodClass;
			}
			catch (ClassNotFoundException e6) {
				System.out.println("NO...");
			}
		}
		try {
			System.out.print("Looking for " + className + "... ");
			goodClass = Class.forName(className);
			System.out.println("YES!");
			return goodClass;
		}
		catch (ClassNotFoundException e7) {
			System.out.println("NO...");
			e7.printStackTrace();
			System.out.println("Required class " + className + " not found, exiting !");
			System.exit(-1);
		}
		return null;
	}

	public Class finder(String cRne) {
		return getGoodClass("org.cocktail.scolarix.serveur.finder.FinderEtudiant", cRne);
	}

	public Class factory() {
		if (garnucheApplication() != null) {
			return factory(garnucheApplication().cRne());
		}
		else {
			return factory(null);
		}
	}

	public Class factory(String cRne) {
		return getGoodClass("org.cocktail.scolarix.serveur.factory.FactoryEtudiant", cRne);
	}

	public Class interfaceEtudiant(String cRne) {
		return getGoodClass("org.cocktail.scolarix.serveur.interfaces.IEtudiant", cRne);
	}

	public EOGarnucheAppliUtilisateur garnucheAppliUtilisateur() {
		return garnucheAppliUtilisateur;
	}

	public void setGarnucheAppliUtilisateur(EOGarnucheAppliUtilisateur garnucheAppliUtilisateur) {
		this.garnucheAppliUtilisateur = garnucheAppliUtilisateur;
	}

	public EOUtilisateur getUtilisateur() {
		return utilisateur;
	}

	public void setUtilisateur(EOUtilisateur utilisateur) {
		this.utilisateur = utilisateur;
	}
}

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

import org.cocktail.fwkcktlajaxwebext.serveur.component.tableview.column.CktlAjaxTableViewColumn;
import org.cocktail.fwkcktlajaxwebext.serveur.component.tableview.column.CktlAjaxTableViewColumnAssociation;
import org.cocktail.inscription.serveur.components.MyComponent;
import org.cocktail.scolarix.serveur.metier.eos.EOEtudiant;
import org.cocktail.scolarix.serveur.metier.eos.EOFraudeur;
import org.cocktail.scolarix.serveur.metier.eos.EOInscDipl;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WODisplayGroup;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

import er.extensions.eof.ERXQ;
import er.extensions.qualifiers.ERXKeyValueQualifier;

public class FraudeursList extends MyComponent {
	private static final String COL_FRAU_NOM = EOFraudeur.FRAU_NOM_KEY;
	private static final String COL_FRAU_PRENOM = EOFraudeur.FRAU_PRENOM_KEY;
	private static final String COL_FRAU_DATE_NAIS = EOFraudeur.FRAU_DATE_NAIS_KEY;
	private static final String COL_FRAU_BORDEREAU = EOFraudeur.FRAU_BORDEREAU_KEY;
	private static final String COL_FRAU_DATE_DEBUT = EOFraudeur.FRAU_DATE_DEBUT_KEY;
	private static final String COL_FRAU_DATE_FIN = EOFraudeur.FRAU_DATE_FIN_KEY;
	private static final String COL_FRAU_RAISON = EOFraudeur.FRAU_RAISON_KEY;
	private static final String COL_FRAU_SANCTION = EOFraudeur.FRAU_SANCTION_KEY;
	private static final String COL_ETUD_NUMERO = EOFraudeur.TO_ETUDIANT_KEY + "." + EOEtudiant.ETUD_NUMERO_KEY;
	private static final String COL_IDIPL_NUMERO = EOFraudeur.TO_INSC_DIPL_KEY + "." + EOInscDipl.IDIPL_NUMERO_KEY;

	/** Colonnes à afficher par défaut. */
	private static final NSArray<String> DEFAULT_COLONNES_KEYS = new NSArray<String>(new String[] { COL_FRAU_NOM, COL_FRAU_PRENOM,
			COL_FRAU_DATE_NAIS, COL_FRAU_BORDEREAU, COL_FRAU_DATE_DEBUT, COL_FRAU_DATE_FIN, COL_FRAU_RAISON, COL_FRAU_SANCTION, COL_ETUD_NUMERO,
			COL_IDIPL_NUMERO });

	/** Binding pour les colonnes a afficher, facultatif, {@link FraudeurList#DEFAULT_COLONNES_KEYS} */
	private static final String BINDING_colonnesKeys = "colonnesKeys";

	private static final NSMutableDictionary<String, CktlAjaxTableViewColumn> _colonnesMap = new NSMutableDictionary<String, CktlAjaxTableViewColumn>();
	private NSArray<CktlAjaxTableViewColumn> colonnes;

	public EOFraudeur unObjet;
	public String filtre;

	public FraudeursList(WOContext context) {
		super(context);
	}

	static {
		add("Nom", COL_FRAU_NOM);
		add("Prénom", COL_FRAU_PRENOM);
		add("Date nais.", COL_FRAU_DATE_NAIS, "%d/%m/%Y", null, null);
		add("Bordereau", COL_FRAU_BORDEREAU);
		add("Date début", COL_FRAU_DATE_DEBUT, "%d/%m/%Y", null, null);
		add("Date fin", COL_FRAU_DATE_FIN, "%d/%m/%Y", null, null);
		add("Raison", COL_FRAU_RAISON);
		add("Sanction", COL_FRAU_SANCTION);
		add("Etudiant", COL_ETUD_NUMERO);
		add("Insc.", COL_IDIPL_NUMERO);
	}

	public WOActionResults filtrer() {
		if (filtre == null) {
			getDisplayGroup().setQualifier(null);
		}
		else {
			ERXKeyValueQualifier nom = ERXQ.contains(COL_FRAU_NOM, filtre);
			ERXKeyValueQualifier prenom = ERXQ.contains(COL_FRAU_PRENOM, filtre);
			getDisplayGroup().setQualifier(ERXQ.or(nom, prenom));
		}
		getDisplayGroup().updateDisplayedObjects();
		return null;
	}

	private WODisplayGroup getDisplayGroup() {
		return (WODisplayGroup) valueForBinding("dg");
	}

	private static void add(String lib, String col) {
		add(lib, col, null, null, null);
	}

	private static void add(String lib, String col, String dateFormat, String numberFormat, String rowCssClass) {
		CktlAjaxTableViewColumn myCol = new CktlAjaxTableViewColumn();
		myCol.setLibelle(lib);
		myCol.setOrderKeyPath(col);
		CktlAjaxTableViewColumnAssociation myAss = new CktlAjaxTableViewColumnAssociation("unObjet." + col, "");
		if (dateFormat != null) {
			myAss.setDateformat(dateFormat);
		}
		if (numberFormat != null) {
			myAss.setNumberformat(numberFormat);
		}
		if (rowCssClass != null) {
			myCol.setRowCssClass(rowCssClass);
		}
		myCol.setAssociations(myAss);
		_colonnesMap.takeValueForKey(myCol, col);
	}

	public NSArray<CktlAjaxTableViewColumn> getColonnes() {
		if (colonnes == null) {
			NSMutableArray<CktlAjaxTableViewColumn> res = new NSMutableArray<CktlAjaxTableViewColumn>();
			NSArray<String> colkeys = getColonnesKeys();
			for (int i = 0; i < colkeys.count(); i++) {
				res.addObject((CktlAjaxTableViewColumn) _colonnesMap.valueForKey(colkeys.objectAtIndex(i)));
			}
			colonnes = res.immutableClone();
		}
		return colonnes;
	}

	private NSArray<String> getColonnesKeys() {
		NSArray<String> keys = DEFAULT_COLONNES_KEYS;
		if (hasBinding(BINDING_colonnesKeys)) {
			String keysStr = (String) valueForBinding(BINDING_colonnesKeys);
			keys = NSArray.componentsSeparatedByString(keysStr, ",");
		}
		return keys;
	}

}

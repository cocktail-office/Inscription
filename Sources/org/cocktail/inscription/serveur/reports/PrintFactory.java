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
package org.cocktail.inscription.serveur.reports;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.util.HashMap;

import org.cocktail.fwkcktlwebapp.server.CktlDataResponse;
import org.cocktail.fwkcktlwebapp.server.database.CktlDataBus;
import org.cocktail.inscription.serveur.Application;
import org.cocktail.inscription.serveur.Session;
import org.cocktail.reporting.jrxml.JrxmlReporter;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOResourceManager;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.jdbcadaptor.JDBCContext;

public class PrintFactory {
	public static final String JASPER_SITUATION = "situation.jasper";

	public static NSData printSituation(Session session, Integer etudNumero) throws Throwable {
		NSMutableDictionary<String, Object> parametres = new NSMutableDictionary<String, Object>();
		parametres.takeValueForKey(etudNumero, "ETUD_NUMERO");

		HashMap<String, Object> aMap = new HashMap<String, Object>();
		aMap.putAll(parametres.hashtable());
		JrxmlReporter reporter = new JrxmlReporter();
		return reporter.printNow(null, getJDBCConnection(), null, getReportLocation(JASPER_SITUATION), aMap, JrxmlReporter.EXPORT_FORMAT_PDF, false,
				true, null);
	}

	public static WOActionResults afficherPdf(NSData pdfData, String fileName) {
		CktlDataResponse resp = new CktlDataResponse();
		if (pdfData != null) {
			resp.setContent(pdfData, CktlDataResponse.MIME_PDF);
			resp.setFileName(fileName);
		}
		else {
			resp.setContent("");
			resp.setHeader("0", "Content-Length");
		}
		return resp.generateResponse();
	}

	private static String getReportLocation(String fileName) {
		Application app = ((Application) Application.application());
		String param = app.config().stringForKey("REPORTS_LOCATION");
		if (param != null) {
			return param + "/" + fileName;
		}
		else {
			WOResourceManager rm = app.resourceManager();
			URL url = rm.pathURLForResourceNamed("Properties", null, null);
			if (url == null) {
				return null;
			}
			String resourcesRoot = new File(url.getPath()).getParent();
			return resourcesRoot.concat("/").concat("report/").concat(fileName);
		}
	}

	private static Connection getJDBCConnection() {
		return ((JDBCContext) CktlDataBus.databaseContext().availableChannel().adaptorChannel().adaptorContext()).connection();
	}
}

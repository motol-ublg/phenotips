/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.internal.controller;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.json.JSONException;

import org.xwiki.component.annotation.Component;

import org.json.JSONObject;

import org.phenotips.configuration.RecordConfigurationManager;
import org.phenotips.data.DictionaryPatientData;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientData;
import org.phenotips.data.PatientDataController;
import static org.phenotips.data.PatientDataController.ERROR_MESSAGE_NO_PATIENT_CLASS;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;

/**
 * Handles textual notes, containing longer prose describing various aspects of the patient's information.
 *
 * @version $Id$
 * @since 1.0M10
 */
@Component(roles = { PatientDataController.class })
@Named("sample_information")
@Singleton
public class SampleInformationController implements PatientDataController<Object>
{

    private static final String[] DATE_IDENTIFIERS = new String[]{
        "sample_date", "illumina_date" };
    private static final String[] STRING_IDENTIFIERS = new String[]{
        "run_number", "udmp_number", "motol_number", "illumina_number" };


    /** Provides access to the underlying data storage. */
    @Inject
    protected DocumentAccessBridge documentAccessBridge;

    /** Logging helper object. */
    @Inject
    private Logger logger;

    /** Provides access to the current execution context. */
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private RecordConfigurationManager configurationManager;

    @Override
    public PatientData<Object> load(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject data = doc.getXObject(Patient.CLASS_REFERENCE);
            if (data == null) {
                return null;
            }
            Map<String, Object> result = new LinkedHashMap<String, Object>();

            for (String identifierName : DATE_IDENTIFIERS) {
                Date date = data.getDateValue(identifierName);
                if (date != null) {
                    result.put(identifierName, date);
                }
            }

            for (String identifierName : STRING_IDENTIFIERS) {
                String value = data.getStringValue(identifierName);
                if (value != null) {
                    result.put(identifierName, value);
                }
            }

            return new DictionaryPatientData<Object>(getName(), result);
        } catch (Exception e) {
            this.logger.error("Could not find requested document or some unforeseen"
                + " error has occurred during controller loading ", e.getMessage());
        }
        return null;
    }

    @Override
    public void save(Patient patient)
    {
        try {
            XWikiDocument doc = (XWikiDocument) this.documentAccessBridge.getDocument(patient.getDocument());
            BaseObject xwikiDataObject = doc.getXObject(Patient.CLASS_REFERENCE);
            if (xwikiDataObject == null) {
                throw new IllegalArgumentException(ERROR_MESSAGE_NO_PATIENT_CLASS);
            }

            PatientData<Object> data = patient.getData(this.getName());
            if (!data.isNamed()) {
                return;
            }

            for (String identifierName : DATE_IDENTIFIERS) {
                xwikiDataObject.setDateValue(identifierName, (Date) data.get(identifierName));
            }

            for (String identifierName : STRING_IDENTIFIERS) {
                xwikiDataObject.setStringValue(identifierName, (String) data.get(identifierName));
            }

            XWikiContext context = this.contextProvider.get();
            String comment = String.format("Updated %s from JSON", this.getName());
            context.getWiki().saveDocument(doc, comment, true, context);
        } catch (Exception e) {
            this.logger.error("Failed to save {}: [{}]", this.getName(), e.getMessage());
        }
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json)
    {
        writeJSON(patient, json, null);
    }

    @Override
    public void writeJSON(Patient patient, JSONObject json, Collection<String> selectedFieldNames)
    {
        PatientData<String> data = patient.getData(getName());
        if (data == null || !data.isNamed()) {
            return;
        }

        JSONObject container = json.optJSONObject(getName());

        Set<String> fieldsToWrite = new HashSet<String>(Arrays.asList(STRING_IDENTIFIERS));
        fieldsToWrite.addAll(Arrays.asList(DATE_IDENTIFIERS));
        if (selectedFieldNames != null) {
            fieldsToWrite.retainAll(selectedFieldNames);
        }

        if (!fieldsToWrite.isEmpty()) {

            if (container == null) {
                // put() is placed here because we want to create the property iff at least one field is set/enabled
                json.put(getName(), new JSONObject());
                container = json.optJSONObject(getName());
            }
            DateFormat dateFormat =
                new SimpleDateFormat(this.configurationManager.getActiveConfiguration().getISODateFormat());

            for (String identifierName : DATE_IDENTIFIERS) {
                container.put(identifierName, dateFormat.format(data.get(identifierName)));
            }

            for (String identifierName : STRING_IDENTIFIERS) {
                container.put(identifierName, data.get(identifierName));
            }
        }
    }

    @Override
    public PatientData<Object> readJSON(JSONObject json)
    {
        if (!json.has(this.getName())) {
            // no data supported by this controller is present in provided JSON
            return null;
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();

        // since the loader always returns dictionary data, this should always be a block.
        Object jsonBlockObject = json.get(this.getName());
        if (!(jsonBlockObject instanceof JSONObject)) {
            return null;
        }
        JSONObject jsonBlock = (JSONObject) jsonBlockObject;

        for (String identifierName : DATE_IDENTIFIERS) {
            extractJsonDateField(jsonBlock, result, identifierName);
        }

        for (String identifierName : STRING_IDENTIFIERS) {
            if (jsonBlock.has(identifierName)) {
                result.put(identifierName, jsonBlock.getString(identifierName));
            }
        }

        return new DictionaryPatientData<Object>(this.getName(), result);
    }

    private void extractJsonDateField(JSONObject jsonBlock,
            Map<String, Object> result,
            String fieldName) throws JSONException {

        if (jsonBlock.has(fieldName)) {
            DateFormat dateFormat = new SimpleDateFormat(this.configurationManager
                    .getActiveConfiguration().getISODateFormat());

            Date val;
            try {
                val = dateFormat.parse(jsonBlock.getString(fieldName));
            } catch (ParseException ex) {
                logger.error("Error parsing date in field " + fieldName, ex);
                val = new Date(0);
            }
            result.put(fieldName, val);
        }
    }

    @Override
    public String getName()
    {
        return "sample_information";
    }
}

package org.phenotips.studies.family;

import org.xwiki.component.annotation.Role;

import java.util.List;

import net.sf.json.JSONObject;

/**
 * Converts the JSON generated by the pedigree into the default format accepted by PhenoTips.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Role
public interface JsonAdapter
{
    /**
     * Takes a JSON object from a pedigree and converts it into a list of patient JSON objects, in the format consumable
     * by the internal PhenoTips patient data model. This is a static method, but cannot be declared as such, given the
     * necessity of injections.
     *
     * @param toConvert cannot be null
     * @return list of JSON objects, each containing patient data of a single patient
     */
    List<JSONObject> convert(JSONObject toConvert);
}
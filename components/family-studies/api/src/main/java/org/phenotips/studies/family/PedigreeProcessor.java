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
public interface PedigreeProcessor
{
    /**
     * Takes a JSON object from a pedigree and converts it into a list of patient JSON objects, in the format consumable
     * by the internal PhenoTips patient data model. This is a static method, but cannot be declared as such, given the
     * necessity of injections.
     *
     * @param pedigree a valid pedigree object.
     * @return list of JSON objects, each containing patient data of a single patient.
     * Returns an empty list if pedigree is null.
     */
    List<JSONObject> convert(Pedigree pedigree);
}

/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.studies.family;

import org.phenotips.Constants;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.QueryException;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Utility methods for manipulating families.
 *
 * @version $Id$
 * @since 1.2RC1
 */
@Role
public interface FamilyUtils
{
    /** XWiki class that represents a family. */
    EntityReference FAMILY_CLASS =
        new EntityReference("FamilyClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);
    /** XWiki class that represents objects that contain a string reference to a family document. */
    EntityReference FAMILY_REFERENCE =
        new EntityReference("FamilyReference", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);
    /** XWiki class that contains rights to XWiki documents. */
    EntityReference RIGHTS_CLASS =
        new EntityReference("XWikiRights", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    /**
     * A wrapper around {@link com.xpn.xwiki.XWiki#getDocument(DocumentReference, XWikiContext)}.
     *
     * @param docRef cannot be null
     * @return the result of calling {@link com.xpn.xwiki.XWiki#getDocument(DocumentReference, XWikiContext)}
     * @throws XWikiException that can be thrown by {@link com.xpn.xwiki.XWiki#getDocument(DocumentReference,
     * XWikiContext)}
     */
    XWikiDocument getDoc(EntityReference docRef) throws XWikiException;

    /**
     * Retrieves a {@link XWikiDocument} given an id.
     *
     * @param id cannot be null
     * @return {@link XWikiDocument}, which could be null
     */
    XWikiDocument getFromDataSpace(String id) throws XWikiException;

    /**
     * Returns a family document tied to the `anchorDoc`. If the `anchorDoc` is a family document, returns `anchorDoc`.
     * If it is a patient document, attempts to get the family document to which the patient belongs.
     *
     * @param anchorDoc a document used as a starting point for searching for a family document
     * @return {@link null} if the `anchorDoc` is not a family document or a patient document (that belongs to a
     * family). Otherwise returns the family document.
     */
    XWikiDocument getFamilyDoc(XWikiDocument anchorDoc) throws XWikiException;

    /**
     * Gets the family document to which the patient with `patientId` belongs to.
     *
     * @param patientId cannot be {@link null}, and should be a valid id
     * @return {@link null} if the patient does not belong to a family, otherwise the family document
     */
    XWikiDocument getFamilyOfPatient(String patientId) throws XWikiException;

    /**
     * Interfaces with the old family studies, which recorded external patient id and relationship to the `patient`.
     *
     * @param patient whose relatives to get
     * @return a collection of relatives stored in the old family stuides in the record of `patient`
     */
    Collection<String> getRelatives(XWikiDocument patient) throws XWikiException;

    /**
     * Resolves the `patientId` into a {@link XWikiDocument}, and then uses {@link #createFamilyDoc(XWikiDocument)} to
     * create a family document.
     *
     * @param patientId patient who will be included into a new family
     * @return a newly created family document with rights set according to {@link #createFamilyDoc(XWikiDocument)} and
     * pedigree of the patient copied to the newly created family.
     */
    XWikiDocument createFamilyDoc(String patientId) throws NamingException, QueryException, XWikiException;

    /**
     * Uses {@link #createFamilyDoc(XWikiDocument, boolean)} to create a new family document. Grants 'Edit' permissions
     * to the family document to all users who have 'Edit' permissions to the patient. Copies the pedigree of the
     * patient into the newly created family document. Uses {@link #createFamilyDoc(XWikiDocument, boolean)} internally
     * to create a family document.
     *
     * @param patient who will be added to the newly created family. If this patient already belongs to a family, the
     * family reference will be silently overwritten, without the patient being removed from the family they belong to.
     * @return a newly created family document
     */
    XWikiDocument createFamilyDoc(XWikiDocument patient) throws NamingException, QueryException, XWikiException;

    /**
     * Allocates an id for a family, and uses a template to create a new document.
     *
     * @param probandDoc document of a patient who will be included as a member in the family members list. This method
     * does not set the patient's family reference.
     * @param save if true calls {@link com.xpn.xwiki.XWiki#saveDocument(XWikiDocument, String, XWikiContext)} on the
     * newly created family document
     * @return a blank family document with no rights or pedigree, but with the `probandDoc` patient included in the
     * list of members
     */
    XWikiDocument createFamilyDoc(XWikiDocument probandDoc, boolean save)
        throws NamingException, QueryException, XWikiException;

    /**
     * Gets the reference to the document of a family that the patient belongs to.
     * @param patientDoc document of the patient whose family is of interest
     * @return null if the patient does not belong to a family; a valid reference otherwise
     * @throws XWikiException
     */
    EntityReference getFamilyReference(XWikiDocument patientDoc) throws XWikiException;

    /**
     * {@see #getFamilyMembers(BaseObject)}
     */
    List<String> getFamilyMembers(XWikiDocument familyDoc) throws XWikiException;

    /**
     * Lists the family members' ids.
     * @param familyObject XWiki object present in family documents which contains the list of family members
     * @return never {@link null}
     * @throws XWikiException
     */
    List<String> getFamilyMembers(BaseObject familyObject) throws XWikiException;

    /**
     * Inserts a reference to a family document into a patient document. Does not save the patient document.
     * @param patientDoc which should be linked to the family
     * @param familyDoc family document which the patient is a member of
     * @param context {@link XWikiContext}
     * @throws XWikiException
     */
    void setFamilyReference(XWikiDocument patientDoc, XWikiDocument familyDoc, XWikiContext context)
        throws XWikiException;

    /**
     * Overwrites the list of family members to the one passed in.
     * @param familyDoc whose members should be updated
     * @param members the new list of family members
     * @throws XWikiException
     */
    void setFamilyMembers(XWikiDocument familyDoc, List<String> members) throws XWikiException;

    /**
     * Filters the permissions of `patientDoc` down to entities (users, groups) that have edit access.
     * @param patientDoc document whose access rights should be parsed
     * @return a list containing a set of users that have edit access (at position 0) and a set of groups that have edit access (at position 1)
     */
    List<Set<String>> getEntitiesWithEditAccess(XWikiDocument patientDoc);
}

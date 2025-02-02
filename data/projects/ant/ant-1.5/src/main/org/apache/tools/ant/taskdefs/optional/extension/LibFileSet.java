/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional.extension;

import org.apache.tools.ant.types.FileSet;

/**
 * LibFileSet represents a fileset containing libraries.
 * Asociated with the libraries is data pertaining to
 * how they are to be handled when building manifests.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision: 1.1 $ $Date: 2002/04/30 03:45:39 $
 */
public class LibFileSet
    extends FileSet
{
    /**
     * Flag indicating whether should include the
     * "Implementation-URL" attribute in manifest.
     * Defaults to false.
     */
    private boolean m_includeURL;

    /**
     * Flag indicating whether should include the
     * "Implementation-*" attributes in manifest.
     * Defaults to false.
     */
    private boolean m_includeImpl;

    /**
     * String that is the base URL for the librarys
     * when constructing the "Implementation-URL"
     * attribute. For instance setting the base to
     * "http://jakarta.apache.org/avalon/libs/" and then
     * including the library "excalibur-cli-1.0.jar" in the
     * fileset will result in the "Implementation-URL" attribute
     * being set to "http://jakarta.apache.org/avalon/libs/excalibur-cli-1.0.jar"
     *
     * Note this is only used if the library does not define
     * "Implementation-URL" itself.
     *
     * Note that this also implies includeURL=true
     */
    private String m_urlBase;

    /**
     * Flag indicating whether should include the
     * "Implementation-URL" attribute in manifest.
     * Defaults to false.
     *
     * @param includeURL the flag
     * @see #m_includeURL
     */
    public void setIncludeUrl( boolean includeURL )
    {
        m_includeURL = includeURL;
    }

    /**
     * Flag indicating whether should include the
     * "Implementation-*" attributes in manifest.
     * Defaults to false.
     *
     * @param includeImpl the flag
     * @see #m_includeImpl
     */
    public void setIncludeImpl( boolean includeImpl )
    {
        m_includeImpl = includeImpl;
    }

    /**
     * Set the url base for fileset.
     *
     * @param urlBase the base url
     * @see #m_urlBase
     */
    public void setUrlBase( String urlBase )
    {
        m_urlBase = urlBase;
    }

    /**
     * Get the includeURL flag.
     *
     * @return the includeURL flag.
     */
    boolean isIncludeURL()
    {
        return m_includeURL;
    }

    /**
     * Get the includeImpl flag.
     *
     * @return the includeImpl flag.
     */
    boolean isIncludeImpl()
    {
        return m_includeImpl;
    }

    /**
     * Get the urlbase.
     *
     * @return the urlbase.
     */
    String getUrlBase()
    {
        return m_urlBase;
    }
}

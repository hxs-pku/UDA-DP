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
package org.apache.tools.ant.taskdefs.optional.extension.resolvers;

import java.io.File;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.optional.extension.Extension;
import org.apache.tools.ant.taskdefs.optional.extension.ExtensionResolver;

/**
 * Resolver that just returns s specified location.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:jeff@socialchange.net.au">Jeff Turner</>
 * @version $Revision: 1.1.2.2 $ $Date: 2002/05/01 11:53:43 $
 */
public class AntResolver
    implements ExtensionResolver
{
    private File m_antfile;
    private File m_destfile;
    private String m_target;

    public void setAntfile( File antfile )
    {
        m_antfile = antfile;
    }

    public void setDestfile( File destfile )
    {
        m_destfile = destfile;
    }

    public void setTarget( final String target )
    {
        m_target = target;
    }

    public File resolve( final Extension extension,
                         final Project project )
        throws BuildException
    {
        validate();

        final Ant ant = (Ant)project.createTask( "ant" );
        ant.setInheritAll( false );
        ant.setAntfile( m_antfile.getName() );

        try
        {
            final File dir =
                m_antfile.getParentFile().getCanonicalFile();
            ant.setDir( dir );
        }
        catch( final IOException ioe )
        {
            throw new BuildException( ioe.getMessage(), ioe );
        }

        if( null != m_target )
        {
            ant.setTarget( m_target );
        }

        ant.execute();

        return m_destfile;
    }

    private void validate()
    {
        if( null == m_antfile )
        {
            final String message = "Must specify Buildfile";
            throw new BuildException( message );
        }

        if( null == m_destfile )
        {
            final String message = "Must specify destination file";
            throw new BuildException( message );
        }
    }

    public String toString()
    {
        return "Ant[" + m_antfile + "==>" + m_destfile + "]";
    }
}

/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.bsf;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.axiom.om.OMElement;
import org.apache.bsf.xml.XMLHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Constants;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.Entry;
import org.apache.synapse.mediators.AbstractMediator;

import java.io.Writer;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * A Synapse mediator that calls a function in any scripting language supported by the BSF.
 * The ScriptMediator supports scripts specified in-line or those loaded through a registry
 * <p/>
 * <pre>
 *    &lt;script [key=&quot;entry-key&quot;]
 *      [function=&quot;script-function-name&quot;] language="javascript|groovy|ruby"&gt
 *      (text | xml)?
 *    &lt;/script&gt;
 * </pre>
 * <p/>
 * <p/>
 * The function is an optional attribute defining the name of the script function to call,
 * if not specified it defaults to a function named 'mediate'. The function takes a single
 * parameter which is the Synapse MessageContext. The function may return a boolean, if it
 * does not then true is assumed.
 */
public class ScriptMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(ScriptMediator.class);
    private static final Log trace = LogFactory.getLog(Constants.TRACE_LOGGER);

    /**
     * The name of the variable made available to the scripting language to access the message
     */
    private static final String MC_VAR_NAME = "mc";

    /**
     * The registry entry key for a script loaded from the registry
     */
    private String key;
    /**
     * The language of the script code
     */
    private String language;
    /**
     * The optional name of the function to be invoked, defaults to mediate
     */
    private String function = "mediate";
    /**
     * The source code of the script
     */
    private String scriptSourceCode;
    /**
     * The BSF engine created to process each message through the script
     */
    protected ScriptEngine scriptEngine;
    /**
     * Does the ScriptEngine support multi-threading
     */
    private boolean multiThreadedEngine;
    /**
     * The compiled script. Only used for inline scripts
     */
    private CompiledScript compiledScript;
    /**
     * The Invocable script. Only used for external scripts
     */
    private Invocable invocableScript;
    /**
     * The BSF helper to convert between the XML representations used by Java and the scripting language
     */
    private XMLHelper xmlHelper;

    /**
     * Create a script mediator for the given language and given script source
     *
     * @param language         the BSF language
     * @param scriptSourceCode the source code of the script
     */
    public ScriptMediator(String language, String scriptSourceCode) {
        this.language = language;
        this.scriptSourceCode = scriptSourceCode;
        initInlineScript();
    }

    /**
     * Create a script mediator for the given language and given script entry key and function
     *
     * @param language the BSF language
     * @param key      the registry entry key to load the script
     * @param function the function to be invoked
     */
    public ScriptMediator(String language, String key, String function) {
        this.language = language;
        this.key = key;
        if (function != null) {
            this.function = function;
        }

        initScriptEngine();
        if (!(scriptEngine instanceof Invocable)) {
            throw new SynapseException("Script engine is not an Invocable engine for language: " + language);
        }
        invocableScript = (Invocable) scriptEngine;
    }

    /**
     * Perform Script mediation
     *
     * @param synCtx the Synapse message context
     * @return the boolean result from the script invocation
     */
    public boolean mediate(MessageContext synCtx) {
        if (log.isDebugEnabled()) {
            log.debug("Script Mediator - mediate() # Language : " + language +
                    (key == null ? " inline script" : " script with key : " + key) +
                    " function : " + function);
        }

        boolean shouldTrace = shouldTrace(synCtx.getTracingState());

        if (shouldTrace) {
            trace.trace("Start : Script mediator # Language : " + language +
                    (key == null ? " inline script" : " script with key : " + key) +
                    " function : " + function);
            trace.trace("Invoking inline script for current message : " + synCtx);
        }

        boolean returnValue;
        if (multiThreadedEngine) {
            returnValue = invokeScript(synCtx);
        } else {
            // TODO: change to use a pool of script engines (requires an update to BSF)
            synchronized (scriptEngine.getClass()) {
                returnValue = invokeScript(synCtx);
            }
        }

        if (shouldTrace) {
            trace.trace("Result message after execution of script : " + synCtx);
            trace.trace("End : Script mediator " + returnValue);
        }

        return returnValue;
    }

    private boolean invokeScript(MessageContext synCtx) {
        boolean returnValue;
        try {

            Object returnObject;
            if (key != null) {
                returnObject = mediateWithExternalScript(synCtx);
            } else {
                returnObject = mediateForInlineScript(synCtx);
            }
            if (returnObject != null && returnObject instanceof Boolean) {
                returnValue = ((Boolean) returnObject).booleanValue();
            } else {
                returnValue = true;
            }

        } catch (ScriptException e) {
            handleException("The Script engine returned an error executing the " +
                (key == null ? "inline " : "external ") + language + " script" +
                (key != null? " : " + key : ""), e);
            returnValue = false;
        }
        return returnValue;
    }

    /**
     * Mediation implementation when the script to be executed should be loaded from the registry
     *
     * @param synCtx the message context
     * @return script result
     * @throws ScriptException
     */
    protected Object mediateWithExternalScript(MessageContext synCtx) throws ScriptException {
        prepareExternalScript(synCtx);
        ScriptMessageContext scriptMC = new ScriptMessageContext(synCtx, xmlHelper);
        return invocableScript.invokeFunction(function, new Object[]{scriptMC});
    }

    /**
     * Perform mediation with static inline script of the given scripting language
     *
     * @param synCtx message context
     * @return true, or the script return value
     * @throws ScriptException
     */
    protected Object mediateForInlineScript(MessageContext synCtx) throws ScriptException {

        ScriptMessageContext scriptMC = new ScriptMessageContext(synCtx, xmlHelper);

        Bindings bindings = scriptEngine.createBindings();
        bindings.put(MC_VAR_NAME, scriptMC);

        Object response;
        if (compiledScript != null) {
            response = compiledScript.eval(bindings);
        } else {
            response = scriptEngine.eval(scriptSourceCode, bindings);
        }

        return response;

    }

    /**
     * Initialise the Mediator for the inline script
     */
    protected void initInlineScript() {
        try {

            initScriptEngine();

            if (scriptEngine instanceof Compilable) {
                compiledScript = ((Compilable)scriptEngine).compile(scriptSourceCode);
            } else {
                // do nothing. If the script enging doesn't support Compilable then
                // the inline script will be evaluated on each invocation
            }

        } catch (ScriptException e) {
            throw new SynapseException("Exception initializing inline script", e);
        }
    }

    /**
     * Prepares the mediator for the invocation of an external script
     * @throws ScriptException
     */
    protected synchronized void prepareExternalScript(MessageContext synCtx) throws ScriptException {

        // TODO: only need this synchronized method for dynamic registry entries. If there was a way
        // to access the registry entry during mediator initialization then for non-dynamic entries
        // this could be done just the once during mediator initialization.

        Entry entry = synCtx.getConfiguration().getEntryDefinition(key);
        boolean needsReload = (entry != null) && entry.isDynamic() && (!entry.isCached() || entry.isExpired());

        if (scriptSourceCode == null || needsReload) {
            Object o = synCtx.getEntry(key);
            if (o instanceof OMElement) {
                scriptSourceCode = ((OMElement) (o)).getText();
            } else if (o instanceof String) {
                scriptSourceCode = (String) o;
            }

            scriptEngine.eval(scriptSourceCode);
        }
    }

    protected void initScriptEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.scriptEngine = manager.getEngineByExtension(language);
        if (scriptEngine == null) {
            throw new SynapseException("No script engine found for language: " + language);
        }
        xmlHelper = XMLHelper.getArgHelper(scriptEngine);

        this.multiThreadedEngine = scriptEngine.getFactory().getParameter("THREADING") != null;
    }

    public String getLanguage() {
        return language;
    }

    public String getKey() {
        return key;
    }

    public String getFunction() {
        return function;
    }

    public String getScriptSrc() {
        return scriptSourceCode;
    }

    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

}

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
package org.apache.synapse.mediators.throttle;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.AbstractMediatorFactory;
import org.apache.synapse.config.xml.SequenceMediatorFactory;
import org.apache.synapse.config.xml.XMLConfigConstants;

import javax.xml.namespace.QName;


/**
 * The Factory for create throttle mediator- key or InLine XMl need to provide
 */

public class ThrottleMediatorFactory extends AbstractMediatorFactory {

    private static final Log log = LogFactory.getLog(ThrottleMediatorFactory.class);

    /**
     * The Tag Name for throttle
     */
    private static final QName TAG_NAME
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "throttle");

    public Mediator createMediator(OMElement elem) {

        ThrottleMediator throttleMediator = new ThrottleMediator();
        OMElement policy = elem.getFirstChildWithName(
                new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "policy"));
        if (policy != null) {
            OMAttribute key = policy.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE, "key"));
            if (key != null) {
                String keyValue = key.getAttributeValue();
                if (keyValue != null && !"".equals(keyValue)) {
                    throttleMediator.setPolicyKey(keyValue);
                } else {
                    handleException("key attribute should have a value ");
                }
            } else {
                OMElement inLine = policy.getFirstElement();
                if (inLine != null) {
                    throttleMediator.setInLinePolicy(inLine);
                }
            }
        } 
        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        processTraceState(throttleMediator,elem);

        String id = elem.getAttributeValue(new QName(XMLConfigConstants.NULL_NAMESPACE, "id"));
        if (id != null) {
            throttleMediator.setId(id.trim());
        }

        SequenceMediatorFactory mediatorFactory = new SequenceMediatorFactory();
        OMAttribute onReject = elem.getAttribute(
                new QName(XMLConfigConstants.NULL_NAMESPACE, XMLConfigConstants.ONREJECT));
        if (onReject != null) {
            String onRejectValue = onReject.getAttributeValue();
            if (onRejectValue != null) {
                throttleMediator.setOnRejectSeqKey(onRejectValue.trim());
            }
        } else {
            OMElement onRejectMediatorElement = elem.getFirstChildWithName(
                    new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, XMLConfigConstants.ONREJECT));
            if (onRejectMediatorElement != null) {
                throttleMediator.setOnRejectMediator(mediatorFactory.createAnonymousSequence(
                        onRejectMediatorElement));
            }
        }
        OMAttribute onAccept = elem.getAttribute(
                new QName(XMLConfigConstants.NULL_NAMESPACE, XMLConfigConstants.ONACCEPT));
        if (onAccept != null) {
            String onAcceptValue = onAccept.getAttributeValue();
            if (onAcceptValue != null) {
                throttleMediator.setOnAcceptSeqKey(onAcceptValue);
            }
        } else {
            OMElement onAcceptMediatorElement = elem.getFirstChildWithName(
                    new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, XMLConfigConstants.ONACCEPT));
            if (onAcceptMediatorElement != null) {
                throttleMediator.setOnAcceptMediator(mediatorFactory.createAnonymousSequence(
                        onAcceptMediatorElement));
            }
        }
        return throttleMediator;
    }

    public QName getTagQName() {
        return TAG_NAME;
    }
}

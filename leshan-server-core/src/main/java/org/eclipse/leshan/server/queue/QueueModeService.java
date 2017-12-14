/*******************************************************************************
 * Copyright (c) 2017 RISE SICS AB.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Carlos Gonzalo Peces - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.server.queue;

import org.eclipse.leshan.server.registration.Registration;

public interface QueueModeService {
	 /**
	     * Add the listener to get notified when the LWM2M client state goes to sleeping or awake.
	     * 
	     * @param listener target to notify
	     */
	    void addListener(QueueModeListener listener);
	
	    /**
	     * Remove the listener previously added. This method has no effect if the given listener is not previously added.
	     * 
	     * @param listener target to be removed.
	     */
	    void removeListener(QueueModeListener listener);
	    
	   /**
	    * Notify all the queue mode listeners that the state of the client has changed from awake to sleeping.
	    */
	    void notifySleeping(Registration registration);
	    
	    /**
	    * Notify all the queue mode listeners that the state of the client has changed from sleeping to awake.
	    */
	    void notifyAwake(Registration registration);
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.connector.petstore;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.extension.api.connection.ConnectionHandler;

import javax.inject.Inject;

public class PetStoreClientConnectionHandler implements ConnectionHandler<PetStoreConnectorConfig, PetStoreClient>, Lifecycle
{

    private int initialise, start, stop, dispose = 0;

    @Inject
    private MuleContext muleContext;

    @Override
    public PetStoreClient connect(PetStoreConnectorConfig config)
    {
        return new PetStoreClient(config.getUsername(), config.getPassword());
    }

    @Override
    public void disconnect(PetStoreClient connection)
    {
        connection.disconnect();
    }

    @Override
    public void dispose()
    {
        dispose++;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        initialise++;
    }

    @Override
    public void start() throws MuleException
    {
        start++;
    }

    @Override
    public void stop() throws MuleException
    {
        stop++;
    }

    public int getInitialise()
    {
        return initialise;
    }

    public int getStart()
    {
        return start;
    }

    public int getStop()
    {
        return stop;
    }

    public int getDispose()
    {
        return dispose;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }
}

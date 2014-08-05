/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.eureka.server.transport.discovery.protocol.asynchronous;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.netflix.eureka.SampleInstanceInfo;
import com.netflix.eureka.interests.Interest;
import com.netflix.eureka.protocol.discovery.AddInstance;
import com.netflix.eureka.protocol.discovery.InterestSetNotification;
import com.netflix.eureka.protocol.discovery.RegisterInterestSet;
import com.netflix.eureka.protocol.discovery.UnregisterInterestSet;
import com.netflix.eureka.server.transport.Context;
import com.netflix.eureka.server.transport.discovery.DiscoveryHandler;
import com.netflix.eureka.transport.Acknowledgement;
import com.netflix.eureka.transport.EurekaTransports;
import com.netflix.eureka.transport.EurekaTransports.Codec;
import com.netflix.eureka.transport.Message;
import com.netflix.eureka.transport.MessageBroker;
import com.netflix.eureka.transport.MessageBrokerServer;
import com.netflix.eureka.transport.UserContent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.subjects.PublishSubject;

import static com.netflix.eureka.registry.SampleInterest.*;
import static org.junit.Assert.*;

/**
 * @author Tomasz Bak
 */
public class AsyncDiscoveryServerTest {

    private MessageBrokerServer brokerServer;
    private MessageBroker brokerClient;
    private AsyncDiscoveryServer discoveryServer;
    private TestDiscoveryHandler handler = new TestDiscoveryHandler();

    @Before
    public void setUp() throws Exception {
        brokerServer = EurekaTransports.tcpDiscoveryServer(0, Codec.Avro).start();
        discoveryServer = new AsyncDiscoveryServer(brokerServer, handler);
        brokerClient = EurekaTransports.tcpDiscoveryClient("localhost", brokerServer.getServerPort(), Codec.Avro).toBlocking().first();
    }

    @After
    public void tearDown() throws Exception {
        if (discoveryServer != null) {
            discoveryServer.shutdown();
        }
    }

    @Test
    public void testRegisterInteresetSet() throws Exception {
        RegisterInterestSet request = new RegisterInterestSet(interestCollectionOf(ZuulVip, DiscoveryApp));

        Observable<Acknowledgement> ackObservable = brokerClient.submitWithAck(new UserContent(request));
        ackObservable.toBlocking().last();

        assertEquals("Invalid interest set received", Arrays.asList(request.getInterestSet()), handler.interestRef.get());
    }

    @Test
    public void testUnregisterInterestSet() throws Exception {
        Observable<Acknowledgement> ackObservable = brokerClient.submitWithAck(new UserContent(new UnregisterInterestSet()));
        ackObservable.toBlocking().last();

        assertTrue("Unregister status not set", handler.unregistered.get());
    }

    @Test
    public void testReceiveUpdates() throws Exception {
        Iterator<Message> updateIterator = brokerClient.incoming().toBlocking().getIterator();
        AddInstance updateAction = new AddInstance(SampleInstanceInfo.DiscoveryServer.build());
        handler.updateSubject.onNext(updateAction);

        Message nextUpdate = updateIterator.next();
        assertEquals("Unexpected update action", updateAction, ((UserContent) nextUpdate).getContent());
    }

    static class TestDiscoveryHandler implements DiscoveryHandler {
        AtomicReference<List<Interest>> interestRef = new AtomicReference<List<Interest>>();

        AtomicBoolean unregistered = new AtomicBoolean();

        PublishSubject<InterestSetNotification> updateSubject = PublishSubject.create();

        @Override
        public Observable<Void> registerInterestSet(Context context, List<Interest> interests) {
            interestRef.set(interests);
            return Observable.empty();
        }

        @Override
        public Observable<Void> unregisterInterestSet(Context context) {
            unregistered.set(true);
            return Observable.empty();
        }

        @Override
        public Observable<InterestSetNotification> updates(Context context) {
            return updateSubject;
        }

        @Override
        public void shutdown(Context context) {

        }
    }
}
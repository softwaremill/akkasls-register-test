package com.softwaremill.test;

import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.Handler;
import com.google.protobuf.Empty;
import com.softwaremill.test.domain.UsersDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Action
public class WelcomeEmailAction {
    private static final Logger LOG = LoggerFactory.getLogger(SubscribeAction.class);

    @Handler
    public Reply<Empty> send(UsersDomain.UserCreated ev) {
        LOG.info("Sending welcome email to {}", ev.getEmail());
        return Reply.message(Empty.getDefaultInstance());
    }
}

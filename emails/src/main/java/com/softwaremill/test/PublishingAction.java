package com.softwaremill.test;

import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.ActionContext;
import com.akkaserverless.javasdk.action.Handler;
import com.softwaremill.test.domain.EmailsDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Action
public class PublishingAction {
    private static final Logger LOG = LoggerFactory.getLogger(PublishingAction.class);

    @Handler
    public EmailsPublishing.EmailAssignedMessage publishEmailAssigned(EmailsDomain.EmailsState ev, ActionContext ctx) {
        String email = ctx.eventSubject().get();
        LOG.info("Publish email assigned for user with email {}", email);
        return EmailsPublishing.EmailAssignedMessage.newBuilder()
                .setUserId(ev.getUserId())
                .setEmail(email)
                .setPasswordSalt(ev.getPasswordSalt())
                .setPasswordHash(ev.getPasswordHash())
                .build();
    }
}

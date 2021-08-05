package com.softwaremill.test;

import com.akkaserverless.javasdk.Context;
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.ServiceCallRef;
import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.ActionContext;
import com.akkaserverless.javasdk.action.Handler;
import com.google.protobuf.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Action
public class SubscribeAction {
    private static final Logger LOG = LoggerFactory.getLogger(SubscribeAction.class);

    private final ServiceCallRef<UsersApi.CreateUserCommand> createUserCommandRef;

    public SubscribeAction(Context ctx) {
        createUserCommandRef = ctx.serviceCallFactory()
                .lookup("com.softwaremill.test.UsersService", "Create", UsersApi.CreateUserCommand.class);
    }

    @Handler
    public Reply<Empty> whenEmailAssigned(UsersSubscribe.EmailAssignedMessage ev, ActionContext ctx) {
        LOG.info("Received a users assigned message for user {}", ev.getEmail());
        return Reply.forward(createUserCommandRef.createCall(
                UsersApi.CreateUserCommand.newBuilder()
                        .setUserId(ev.getUserId())
                        .setEmail(ev.getEmail())
                        .setPasswordSalt(ev.getPasswordSalt())
                        .setPasswordHash(ev.getPasswordHash())
                        .build()
        ));
    }
}

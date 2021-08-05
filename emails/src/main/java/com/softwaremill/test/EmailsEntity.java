package com.softwaremill.test;

import com.akkaserverless.javasdk.*;
import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.akkaserverless.javasdk.eventsourcedentity.CommandHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.google.protobuf.Empty;
import com.softwaremill.test.domain.EmailsDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.UUID;

@EventSourcedEntity(entityType = "emails")
public class EmailsEntity {
    private static final Logger LOG = LoggerFactory.getLogger(EmailsEntity.class);
    private static final SecureRandom RANDOM;

    static {
        try {
            RANDOM = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private final String email;
    private boolean assigned;

    public EmailsEntity(@EntityId String email) {
        this.email = email;
    }

    @CommandHandler
    public Reply<EmailsApi.RegisterUserResult> register(EmailsApi.RegisterUserCommand cmd, CommandContext ctx) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (assigned) {
            LOG.info("Email {} is already taken", email);
            throw ctx.fail("Email is already taken");
        } else {
            String userId = UUID.randomUUID().toString();

            // hashing the password - from https://stackoverflow.com/questions/2860943/how-can-i-hash-a-password-in-java
            byte[] salt = new byte[16];
            RANDOM.nextBytes(salt);
            KeySpec spec = new PBEKeySpec(cmd.getPassword().toCharArray(), salt, 65536, 128);
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = f.generateSecret(spec).getEncoded();
            Base64.Encoder enc = Base64.getEncoder();
            String passwordSalt = enc.encodeToString(salt);
            String passwordHash = enc.encodeToString(hash);

            // emitting the event
            LOG.info("Email {} is free, assigning to user id {}", email, userId);
            ctx.emit(EmailsDomain.EmailAssigned.newBuilder()
                    .setUserId(userId)
                    .setPasswordSalt(passwordSalt)
                    .setPasswordHash(passwordHash)
                    .build());

            return Reply.message(EmailsApi.RegisterUserResult.newBuilder().setUserId(userId).build());
        }
    }

    @EventHandler
    public void emailAssigned(EmailsDomain.EmailAssigned ev) {
        assigned = true;
    }
}

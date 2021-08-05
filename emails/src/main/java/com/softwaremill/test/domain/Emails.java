/* This code was initialised by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.softwaremill.test.domain;

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.valueentity.*;
import com.softwaremill.test.EmailsApi;
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

@ValueEntity(entityType = "emails")
public class Emails extends AbstractEmails {
    private static final Logger LOG = LoggerFactory.getLogger(Emails.class);
    private static final SecureRandom RANDOM;

    static {
        try {
            RANDOM = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private final String email;

    public Emails(@EntityId String email) {
        this.email = email;
    }

    @Override
    public Reply<EmailsApi.RegisterUserResult> register(EmailsApi.RegisterUserCommand cmd, CommandContext<EmailsDomain.EmailsState> ctx) {
        var current = ctx.getState();
        if (current.isPresent() && !current.get().getUserId().isEmpty()) {
            LOG.info("Email {} is already taken", email);
            throw ctx.fail("Email is already taken");
        } else try {
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
            ctx.updateState(EmailsDomain.EmailsState.newBuilder()
                    .setUserId(userId)
                    .setPasswordSalt(passwordSalt)
                    .setPasswordHash(passwordHash)
                    .build());

            return Reply.message(EmailsApi.RegisterUserResult.newBuilder().setUserId(userId).build());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
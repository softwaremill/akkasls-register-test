package com.softwaremill.test;

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.akkaserverless.javasdk.eventsourcedentity.CommandHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.google.protobuf.Empty;
import com.softwaremill.test.domain.UsersDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

@EventSourcedEntity(entityType = "users")
public class UsersEntity {
    private static final Logger LOG = LoggerFactory.getLogger(UsersEntity.class);

    private final String userId;
    private String email;
    private byte[] passwordSalt;
    private byte[] passwordHash;

    public UsersEntity(@EntityId String userId) {
        this.userId = userId;
    }

    @CommandHandler
    public Empty create(UsersApi.CreateUserCommand cmd, CommandContext ctx) {
        // emitting the event
        LOG.info("Creating user {} with email {}", userId, cmd.getEmail());
        ctx.emit(UsersDomain.UserCreated.newBuilder()
                .setEmail(cmd.getEmail())
                .setPasswordSalt(cmd.getPasswordSalt())
                .setPasswordHash(cmd.getPasswordHash())
                .build());
        return Empty.getDefaultInstance();
    }

    @EventHandler
    public void userCreated(UsersDomain.UserCreated ev) {
        email = ev.getEmail();
        passwordSalt = Base64.getDecoder().decode(ev.getPasswordSalt());
        passwordHash = Base64.getDecoder().decode(ev.getPasswordHash());
    }

    @CommandHandler
    public Empty authenticate(UsersApi.AuthenticateUserCommand cmd, CommandContext ctx) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // hashing the incoming password
        KeySpec spec = new PBEKeySpec(cmd.getPassword().toCharArray(), passwordSalt, 65536, 128);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] incomingPasswordHash = f.generateSecret(spec).getEncoded();

        if (MessageDigest.isEqual(passwordHash, incomingPasswordHash)) {
            LOG.info("User {} authenticated", userId);
            return Empty.getDefaultInstance();
        } else {
            LOG.info("User {} authentication failed", userId);
            throw ctx.fail("Incorrect password");
        }
    }
}

package com.webank.wecross.stub.fabric.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricUser implements User {

    private Logger logger = LoggerFactory.getLogger(FabricUser.class);
    private FabricConfig fabricConfig;

    public FabricUser(FabricConfig fabricConfig) {
        this.fabricConfig = fabricConfig;
    }

    @Override
    public String getName() {
        return fabricConfig.getOrgUserName();
    }

    @Override
    public Set<String> getRoles() {
        return new HashSet<String>();
    }

    @Override
    public String getAccount() {
        return "";
    }

    @Override
    public String getAffiliation() {
        return "";
    }

    @Override
    public Enrollment getEnrollment() {
        return new Enrollment() {
            @Override
            public PrivateKey getKey() {
                try {
                    String privateKeyContent =
                            new String(
                                    Files.readAllBytes(
                                            Paths.get(fabricConfig.getOrgUserKeyFile())));
                    privateKeyContent =
                            privateKeyContent
                                    .replaceAll("\\n", "")
                                    .replace("-----BEGIN PRIVATE KEY-----", "")
                                    .replace("-----END PRIVATE KEY-----", "");
                    KeyFactory kf = KeyFactory.getInstance("EC");
                    PKCS8EncodedKeySpec keySpecPKCS8 =
                            new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent));
                    return kf.generatePrivate(keySpecPKCS8);
                } catch (IOException e) {
                    logger.error(
                            "getKey failed path:{} errmsg:{}", fabricConfig.getOrgUserKeyFile(), e);
                    return null;
                } catch (GeneralSecurityException e) {
                    logger.error(
                            "getKey failed path:{} errmsg:{}", fabricConfig.getOrgUserKeyFile(), e);
                    return null;
                }
            }

            @Override
            public String getCert() {
                try {
                    return new String(
                            Files.readAllBytes(Paths.get(fabricConfig.getOrgUserCertFile())));
                } catch (IOException e) {
                    logger.error(
                            "getKey failed path:{} errmsg:{}",
                            fabricConfig.getOrgUserCertFile(),
                            e);
                    return "";
                }
            }
        };
    }

    @Override
    public String getMspId() {
        return this.fabricConfig.getMspId();
    }
}

package com.jomariabejo.connectly_api.util;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Base64;

public class KeyGeneratorUtil {
    public static void main(String[] args) {
        Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String base64EncodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("Base64 Encoded Key: " + base64EncodedKey);
    }
}

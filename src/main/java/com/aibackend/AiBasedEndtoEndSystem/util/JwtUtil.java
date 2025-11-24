package com.aibackend.AiBasedEndtoEndSystem.util;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512);


    @Data
    public static class Token {
        private String authKey;
    }

//    public JwtUtil(Key key) {
//        this.key = key;
//    }

//    @Autowired
//    public JwtUtil(JwtProvider keyProvider) {
//        this.key = keyProvider.getKey();
//    }

    public Key getKey() {
        return key;
    }


    private Integer adminTokenExpiryInSeconds = 72 * 60 * 60;

    //    public Token generateClientToken(UserDTO userDTO)
//            throws Exception {
//        log.info("generate client Token request : {}", userDTO);
//        if (ObjectUtils.isEmpty(userDTO.getId())) {
//            throw new BadException("Invalid user");
//        }
//        String tokenKey = UUID.randomUUID().toString();
//        Integer expiryTimeInSeconds = adminTokenExpiryInSeconds;
//        log.debug("generated the token Id : {}, {}", tokenKey, userDTO);
//        Token token = new Token();
//        token.setAuthKey(tokenKey);
//        return token;
//    }
    public Token generateClientToken(UserDTO userDTO) throws Exception {
        log.info("generate client Token request : {}", userDTO);
        if (ObjectUtils.isEmpty(userDTO.getId())) {
            throw new BadException("Invalid user");
        }

        long now = System.currentTimeMillis();
        long expiryMillis = now + adminTokenExpiryInSeconds * 1000L;

        String jwt = Jwts.builder()
                .setSubject(userDTO.getId()) // user ID as subject
                .claim("userName", userDTO.getUsername())
                .claim("userEmail", userDTO.getUserEmail())
                .claim("userMobileNumber", userDTO.getMobileNumber())
                .claim("role",userDTO.getRole())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expiryMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        Token token = new Token();
        token.setAuthKey(jwt);
        return token;
    }


    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUserObjectId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject(); // this is the user ID
    }

//    public String extractUserObjectId(String token) {
//        Claims claims = Jwts.parserBuilder()
//                .setSigningKey(key)
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//
//        return claims.getSubject();
//    }
}

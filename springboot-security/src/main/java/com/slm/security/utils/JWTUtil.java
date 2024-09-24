package com.slm.security.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

public class JWTUtil {

    //加密算法
    private final static SecureDigestAlgorithm<SecretKey, SecretKey> ALGORITHM = Jwts.SIG.HS256;

    //私钥 / 生成签名的时候使用的秘钥secret，一般可以从本地配置文件中读取，切记这个秘钥不能外露，只在服务端使用，在任何场景都不应该流露出去。
    // 一旦客户端得知这个secret, 那就意味着客户端是可以自我签发jwt了。
    private final static String secret = "$2a$10$kmcxrdyU/UbcXdtQfFu/d.7gKwo20N2z/lQ2nCw9wWZcKepYoLtn.";
    public static final SecretKey KEY = Keys.hmacShaKeyFor(secret.getBytes());

    // 过期时间（单位秒）
    private final static Long access_token_expiration = 24 * 60 * 60L;

    //jwt签发者
    private final static String jwt_iss = "spzhang";

    //jwt所有人
    private final static String subject = "zhangsp";

    /**
     * 创建 JWT
     *
     * @return 返回生成的jwt token
     */
    public static String generateJwtToken(String username, String password) {
        return Jwts.builder()
                .header()
                .add("typ", "JWT")
                .add("alg", "HS256")
                .and()
                .claim("username", username)
                // 令牌ID
                .id(UUID.randomUUID().toString())
                // 过期日期
                .expiration(new Date(System.currentTimeMillis() + access_token_expiration * 1000))
                // 签发时间
                .issuedAt(new Date())
                // 主题
                .subject(subject)
                // 签发者
                .issuer(jwt_iss)
                // 签名
                .signWith(KEY, ALGORITHM)
                .compact();
    }

    /**
     * 验证 JWT 并从中获取载荷信息
     *
     * @param jwt
     * @return
     */
    public static Jws<Claims> getClaimsFromJwt(String jwt) {
        Jws<Claims> claims = null;
        try {
            claims = Jwts.parser().verifyWith(KEY).build().parseClaimsJws(jwt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return claims;
    }

}

package com.xy.course_scheduling.utils;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.function.Function;

//jwt 工具类
@Component
public class JwtUtil {
    private final String SECRET_KEY;
    private final long EXPIRE_TIME;
    public JwtUtil(
            @Value("${jwt.secret}")String secretKey,
            @Value("${jwt.expiration}")long expireTime
    ) {
        this.SECRET_KEY = secretKey;
        this.EXPIRE_TIME = expireTime;
    }
    //签发token
    public String createToken(String username,String role,String name) {
        HashMap<String, Object> claim = new HashMap<>();
        claim.put("role", role);
        claim.put("name", name);
        return Jwts.builder()
                .setClaims(claim)//存储角色
                .setSubject(username)//存储用户名
                .setIssuedAt(new Date())//签发时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))//过期时间
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)//签名算法和密钥
                .compact();//compact()方法将JWT压缩成字符串
    }
    //解析token
    public Claims parseToken(String token) {
        Claims body = Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
        return body;
    }
    //从Claims中解析指定的字段
    public <T> T getClaim(String token, Function<Claims, T> claimsTFunction) {
        Claims claims = parseToken(token);
        return claimsTFunction.apply(claims);
    }
    //从Claims中解析用户名
    public String getUsername(String token) {
        return getClaim(token, Claims::getSubject);
    }
    //从 Claims中解析姓名
     public String getName(String token) {
        return getClaim(token, claims -> claims.get("name", String.class));
    }
    //从Claims中解析角色
    public String getRole(String token) {
        return getClaim(token, claims -> claims.get("role", String.class));
    }
    //解析签发时间
    public Date getIssuedAt(String token) {
        return getClaim(token, Claims::getIssuedAt);
    }
    //解析过期时间
    public Date getExpiration(String token) {
        return getClaim(token, Claims::getExpiration);
    }
    //验证token是否过期
    public boolean isExpired(String token) {
        return getExpiration(token).before(new Date());
    }
    //验证token是否正确
    public boolean validateToken(String token, UserDetails userDetails) {
        String username = getUsername(token);
        return (username.equals(userDetails.getUsername()) && !isExpired(token));
    }
    //检查token是否需要续期，到期前一天如果有访问则续期
    public boolean checkRenewal(String token) {

        long time = getExpiration(token).getTime();
        long now = System.currentTimeMillis();
        System.out.println("剩余时间："+(time - now));
        return time - now < 24*60*60*1000;
    }
}

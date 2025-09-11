package co.com.crediya.solicitudes.jwt;

import org.springframework.stereotype.Component;

@Component
public class JwtErrorDetector {

    public boolean isJwtRelatedError(Throwable error) {
        return error instanceof io.jsonwebtoken.JwtException ||
               error instanceof io.jsonwebtoken.ExpiredJwtException ||
               error instanceof io.jsonwebtoken.MalformedJwtException ||
               error instanceof io.jsonwebtoken.UnsupportedJwtException ||
               error instanceof io.jsonwebtoken.security.SecurityException ||
               error instanceof IllegalArgumentException;
    }
}

package com.mobile.aura.domain.auth;

import com.mobile.aura.mapper.RefreshTokenMapper;

/**
 * Value object that holds both the RefreshToken entity and the raw token string
 * The raw token is sent to the client, while only the hash is stored in the database
 */
public record RefreshTokenPair(RefreshToken token, String rawToken) {

    /**
     * Save the token to database
     */
    public void saveTo(RefreshTokenMapper mapper) {
        token.saveOrUpdate(mapper);
    }
}

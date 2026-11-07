package com.ledgerline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ledgerline.service.AccountAuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.mockito.Mockito;

class AccountAuthorizationServiceTest {

    @Test
    void authorizationUsesAuthenticatedUserIdAndRejectsOtherAccounts() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        AccountAuthorizationService authorizationService = new AccountAuthorizationService(jdbcTemplate);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "41", null, java.util.List.of());
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(0);

        assertFalse(authorizationService.canViewAccount(authentication, 77L));

        verify(jdbcTemplate).queryForObject(
                anyString(), eq(Integer.class), eq(77L), eq(41L), eq("OWNER"), eq("VIEW"), eq("TRANSFER"));
    }

    @Test
    void transferPermissionAllowsOnlyTransferOrOwnerPermission() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        AccountAuthorizationService authorizationService = new AccountAuthorizationService(jdbcTemplate);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                "41", null, java.util.List.of());
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class))).thenReturn(1);

        assertTrue(authorizationService.canTransferFromAccount(authentication, 77L));
    }
}

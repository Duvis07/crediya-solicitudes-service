package co.com.crediya.solicitudes.jwt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AllowedRolesTest {

    @Test
    void testAllowedRolesEnumValues() {
        // Verify all expected roles exist
        AllowedRoles[] roles = AllowedRoles.values();
        
        assertEquals(3, roles.length);
        assertTrue(containsRole(roles, AllowedRoles.ADMIN));
        assertTrue(containsRole(roles, AllowedRoles.ASESOR));
        assertTrue(containsRole(roles, AllowedRoles.APPLICANT));
    }

    @Test
    void testRoleNames() {
        assertEquals("ADMIN", AllowedRoles.ADMIN.name());
        assertEquals("ASESOR", AllowedRoles.ASESOR.name());
        assertEquals("APPLICANT", AllowedRoles.APPLICANT.name());
    }

    @Test
    void testValueOf() {
        assertEquals(AllowedRoles.ADMIN, AllowedRoles.valueOf("ADMIN"));
        assertEquals(AllowedRoles.ASESOR, AllowedRoles.valueOf("ASESOR"));
        assertEquals(AllowedRoles.APPLICANT, AllowedRoles.valueOf("APPLICANT"));
    }

    @Test
    void testValueOfThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            AllowedRoles.valueOf("INVALID_ROLE");
        });
    }

    @Test
    void testEnumOrdinal() {
        assertEquals(0, AllowedRoles.ADMIN.ordinal());
        assertEquals(1, AllowedRoles.ASESOR.ordinal());
        assertEquals(2, AllowedRoles.APPLICANT.ordinal());
    }

    private boolean containsRole(AllowedRoles[] roles, AllowedRoles targetRole) {
        for (AllowedRoles role : roles) {
            if (role == targetRole) {
                return true;
            }
        }
        return false;
    }
}

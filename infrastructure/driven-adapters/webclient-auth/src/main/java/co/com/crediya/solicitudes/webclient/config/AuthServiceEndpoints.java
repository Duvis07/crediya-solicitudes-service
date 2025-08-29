package co.com.crediya.solicitudes.webclient.config;


public final class AuthServiceEndpoints {

    private AuthServiceEndpoints() {}

    public static final String API_BASE = "/api/v1";
    public static final String USERS_BASE = API_BASE + "/usuarios";
    public static final String USER_BY_DOCUMENT = USERS_BASE + "/{documentId}";


    public static String getUserByDocumentUrl(String baseUrl) {
        return baseUrl + USER_BY_DOCUMENT;
    }
}

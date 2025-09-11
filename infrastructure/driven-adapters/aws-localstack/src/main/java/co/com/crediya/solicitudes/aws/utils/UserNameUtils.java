package co.com.crediya.solicitudes.aws.utils;

public final class UserNameUtils {

    private UserNameUtils() {
        // Utility class - prevent instantiation
    }


    public static String buildFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return "Cliente";
        }
        if (firstName == null) {
            return lastName.trim();
        }
        if (lastName == null) {
            return firstName.trim();
        }
        return (firstName.trim() + " " + lastName.trim()).trim();
    }
}

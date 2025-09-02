package co.com.crediya.solicitudes.api.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UserNameUtils {

    public static String buildFullName(String firstName, String lastName) {
        if (firstName != null && lastName != null) {
            return firstName.trim() + " " + lastName.trim();
        } else if (firstName != null) {
            return firstName.trim();
        } else if (lastName != null) {
            return lastName.trim();
        }
        return "";
    }
}

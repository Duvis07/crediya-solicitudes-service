package co.com.crediya.solicitudes.api.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UserNameUtils {

    public static String buildFullName(String firstName, String lastName) {
        String trimmedFirst = (firstName != null) ? firstName.trim() : "";
        String trimmedLast = (lastName != null) ? lastName.trim() : "";
        
        if (!trimmedFirst.isEmpty() && !trimmedLast.isEmpty()) {
            return trimmedFirst + " " + trimmedLast;
        } else if (!trimmedFirst.isEmpty()) {
            return trimmedFirst;
        } else if (!trimmedLast.isEmpty()) {
            return trimmedLast;
        }
        return "";
    }
}

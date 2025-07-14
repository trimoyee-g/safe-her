package com.place.PlaceMicroservice.util;

public class AccessControlUtil {

    public static boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public static boolean isSelf(String requesterId, String targetId) {
        return requesterId != null && requesterId.equals(targetId);
    }

    public static boolean isAdminOrSelf(String role, String requesterId, String targetId) {
        return isAdmin(role) || isSelf(requesterId, targetId);
    }
}

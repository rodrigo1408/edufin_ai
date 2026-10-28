package br.finedu.security;

import java.util.EnumSet;
import java.util.Set;

public enum Role {
    USER(EnumSet.noneOf(Permission.class)),
    EDUCATOR(EnumSet.of(Permission.ADMIN_AREA, Permission.CONTENT_MANAGE)),
    MODERATOR(EnumSet.of(Permission.ADMIN_AREA, Permission.FORUM_MODERATE)),
    ADMIN(EnumSet.allOf(Permission.class));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> permissions() {
        return permissions;
    }

    public boolean has(Permission permission) {
        return permissions.contains(permission);
    }
}

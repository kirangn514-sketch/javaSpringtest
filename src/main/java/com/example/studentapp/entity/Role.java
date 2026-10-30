package com.example.studentapp.entity;

/**
 * The permission group a user belongs to. The role is stored as text in app_users.role
 * and copied into the JWT as the "role" claim.
 *
 *   ADMIN:    full access to every endpoint
 *   HR:       list all students and read one student
 *   EMPLOYEE: read only the student records linked to their own account
 */
public enum Role {
    ADMIN,
    HR,
    EMPLOYEE
}

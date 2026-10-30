package com.example.studentapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity that maps to the "students" table in the SQLite database.
 *
 * Each instance is one row. Each field is one column.
 * This class is an internal database model. Controllers never return it directly.
 * The DTO classes are used for the API instead.
 */
@Entity
@Table(name = "students")
public class Student {

    /**
     * @Id marks the primary key (the column that uniquely identifies a row).
     * @GeneratedValue(IDENTITY) lets the database assign the next number.
     * SQLite supports this through AUTOINCREMENT, so no sequence object is needed.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** nullable = false makes the column NOT NULL in the database. */
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String subject;

    /** Optional. Without nullable = false, the column accepts NULL. */
    private String address;

    private Integer age;

    /**
     * The user this student record belongs to. Employees may read only the records they own.
     * Optional: a student with no owner is visible to ADMIN and HR only.
     * Many students can share one owner, so this is a @ManyToOne relationship.
     */
    @ManyToOne
    @JoinColumn(name = "owner_user_id")
    private AppUser owner;

    /** updatable = false: the creation time is written once and never changed. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * JPA requires a no-argument constructor so it can create objects when reading rows.
     * It is public here because the mapper (in another package) also uses it.
     */
    public Student() {
    }

    /**
     * @PrePersist runs just before the first INSERT.
     * Instant.now() is in UTC, which is the standard for stored timestamps.
     */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** @PreUpdate runs just before each UPDATE. */
    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and setters. JPA and the mapper read and write fields through these.
    // There is no setId(): the database generates the ID, so the application never sets it.

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

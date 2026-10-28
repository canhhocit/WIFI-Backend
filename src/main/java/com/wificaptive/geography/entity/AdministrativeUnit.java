package com.wificaptive.geography.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "administrative_units")
public class AdministrativeUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private AdministrativeUnit parent;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer level; // 1: City, 2: District, 3: Ward/Commune

    @Column(nullable = false, length = 255)
    private String path;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, MERGED, SPLIT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_to_id")
    private AdministrativeUnit mergedTo;

    @Column(length = 255)
    private String notes;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<AdministrativeUnit> children = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public AdministrativeUnit() {}

    public AdministrativeUnit(String code, String name, Integer level, String path) {
        this.code = code;
        this.name = name;
        this.level = level;
        this.path = path;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AdministrativeUnit getParent() { return parent; }
    public void setParent(AdministrativeUnit parent) { this.parent = parent; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public AdministrativeUnit getMergedTo() { return mergedTo; }
    public void setMergedTo(AdministrativeUnit mergedTo) { this.mergedTo = mergedTo; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<AdministrativeUnit> getChildren() { return children; }
    public void setChildren(List<AdministrativeUnit> children) { this.children = children; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}


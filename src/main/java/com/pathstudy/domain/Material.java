package com.pathstudy.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A learning material (video / document / link) that a teacher or admin attaches
 * to a lesson. Stored as a URL — for the MVP teachers paste YouTube / Drive / PDF
 * links (no file storage needed, works on ephemeral hosts).
 */
@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialType type = MaterialType.LINK;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    private String createdByEmail;

    @Column(nullable = false)
    private int orderIndex = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** For YouTube video URLs, the embeddable URL; otherwise null (render as a link). */
    @Transient
    public String getEmbedUrl() {
        if (type != MaterialType.VIDEO || url == null) {
            return null;
        }
        String id = youTubeId(url);
        return id == null ? null : "https://www.youtube.com/embed/" + id;
    }

    private static String youTubeId(String u) {
        try {
            if (u.contains("youtu.be/")) {
                return trimId(u.substring(u.indexOf("youtu.be/") + 9));
            }
            if (u.contains("/embed/")) {
                return trimId(u.substring(u.indexOf("/embed/") + 7));
            }
            if (u.contains("v=")) {
                return trimId(u.substring(u.indexOf("v=") + 2));
            }
        } catch (RuntimeException ignore) {
            // fall through
        }
        return null;
    }

    private static String trimId(String s) {
        int i = 0;
        while (i < s.length() && "&?/ ".indexOf(s.charAt(i)) < 0) {
            i++;
        }
        String id = s.substring(0, i);
        return id.isEmpty() ? null : id;
    }
}

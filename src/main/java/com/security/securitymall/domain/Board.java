package com.security.securitymall.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime regDate;
    private int views = 0;
    private String writer;
    private String writerUsername;

    public Board() {}

    @PrePersist
    public void onCreate() {
        this.regDate = LocalDateTime.now();
    }

    // 날짜를 yyyy-MM-dd HH:mm 형식으로 변환하여 반환
    public String getFormattedDate() {
        if (this.regDate == null) return "-";
        return this.regDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    // Getter & Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getRegDate() { return regDate; }
    public void setRegDate(LocalDateTime regDate) { this.regDate = regDate; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public String getWriter() { return writer; }
    public void setWriter(String writer) { this.writer = writer; }

    public String getWriterUsername() { return writerUsername; }
    public void setWriterUsername(String writerUsername) { this.writerUsername = writerUsername; }
}
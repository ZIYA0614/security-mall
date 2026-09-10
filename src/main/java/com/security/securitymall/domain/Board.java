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

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    private String writer;         // 작성자 이름
    private String writerUsername; // 작성자 아이디
    private String createdDate;    // 작성일
    private Integer views = 0;     // 조회수 (NullPointerException 방지를 위해 Integer 사용)

    @PrePersist
    public void prePersist() {
        if (this.createdDate == null) {
            this.createdDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
        if (this.views == null) {
            this.views = 0;
        }
    }

    public Board() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getWriter() { return writer; }
    public void setWriter(String writer) { this.writer = writer; }
    public String getWriterUsername() { return writerUsername; }
    public void setWriterUsername(String writerUsername) { this.writerUsername = writerUsername; }
    public String getCreatedDate() { return createdDate == null ? "" : createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
    public Integer getViews() { return views == null ? 0 : views; }
    public void setViews(Integer views) { this.views = views; }
}
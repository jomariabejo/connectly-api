package com.jomariabejo.connectly_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "follow", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"follower_id", "following_id"})
})
public class Follow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "approved", nullable = false)
    private Boolean approved = false;

    @Column(name = "request_status", length = 20)
    @Enumerated(EnumType.STRING)
    private FollowRequestStatus requestStatus = FollowRequestStatus.PENDING;

    public Follow() {}

    public Follow(User follower, User following) {
        this.follower = follower;
        this.following = following;
        this.approved = false;
        this.requestStatus = FollowRequestStatus.PENDING;
    }

    public enum FollowRequestStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}

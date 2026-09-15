package com.bloodbridge;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="inbox_threads", indexes={
 @Index(name="idx_inbox_owner_updated",columnList="owner_id,updated_at"),
 @Index(name="idx_inbox_admin_unread",columnList="admin_unread,updated_at")})
public class InboxThread {
 @Id @Column(length=36) public String id=UUID.randomUUID().toString();
 @Column(name="owner_id",length=36,nullable=false) public String ownerId;
 @Column(name="source_key",length=120,nullable=false,unique=true) public String sourceKey;
 @Column(length=20,nullable=false) public String kind;
 @Column(length=180,nullable=false) public String subject;
 @Column(name="draft_text",length=4000,nullable=false) public String draftText="";
 @Column(name="draft_revision",nullable=false) public int draftRevision;
 @Column(name="sent_revision",nullable=false) public int sentRevision;
 @Column(name="admin_unread",nullable=false) public boolean adminUnread=true;
 @Column(name="member_unread",nullable=false) public boolean memberUnread=true;
 @Column(name="created_at",nullable=false) public long createdAt=System.currentTimeMillis();
 @Column(name="updated_at",nullable=false) public long updatedAt=createdAt;
 protected InboxThread(){}
}

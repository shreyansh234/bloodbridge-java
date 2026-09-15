package com.bloodbridge;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="inbox_messages", uniqueConstraints=@UniqueConstraint(name="uq_inbox_message_key",columnNames={"thread_id","client_key"}),indexes=@Index(name="idx_inbox_message_thread",columnList="thread_id,created_at"))
public class InboxMessage {
 @Id @Column(length=36) public String id=UUID.randomUUID().toString();
 @Column(name="thread_id",length=36,nullable=false) public String threadId;
 @Column(name="client_key",length=80,nullable=false) public String clientKey;
 @Column(name="sender_id",length=36,nullable=false) public String senderId="";
 @Column(name="sender_role",length=20,nullable=false) public String senderRole;
 @Column(name="message_body",length=6000,nullable=false) public String body;
 @Column(name="created_at",nullable=false) public long createdAt;
 protected InboxMessage(){}
}

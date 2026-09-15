package com.bloodbridge;
import jakarta.persistence.*;
@Entity @Table(name="email_notices")
public class EmailNotice {
 @Id @Column(length=36) public String id;
 @Column(length=200,nullable=false) public String subject;
 @Column(name="message_body",length=6000,nullable=false) public String messageBody;
 @Column(length=20,nullable=false) public String status="pending";
 @Column(nullable=false) public int attempts;
 @Column(name="next_attempt",nullable=false) public long nextAttempt;
 @Column(name="provider_id",length=180,nullable=false) public String providerId="";
 @Column(name="last_error",length=180,nullable=false) public String lastError="";
 protected EmailNotice(){}
}

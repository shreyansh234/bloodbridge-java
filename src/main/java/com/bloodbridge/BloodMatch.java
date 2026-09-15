package com.bloodbridge;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="blood_matches",indexes={@Index(name="idx_match_donor",columnList="donor_id,created_at"),@Index(name="idx_match_receiver",columnList="receiver_id,created_at")})
public class BloodMatch {
 @Id @Column(length=36) public String id=UUID.randomUUID().toString();
 @Column(name="donor_id",length=36,nullable=false) public String donorId;
 @Column(name="receiver_id",length=36,nullable=false) public String receiverId;
 @Column(name="patient_name",length=80,nullable=false) public String patientName;
 @Column(length=120,nullable=false) public String hospital;
 @Column(name="blood_group",length=3,nullable=false) public String bloodGroup;
 @Column(length=80,nullable=false) public String state;
 @Column(length=80,nullable=false) public String city;
 @Column(length=500,nullable=false) public String note="";
 @Column(name="donor_approved",nullable=false) public boolean donorApproved;
 @Column(name="receiver_approved",nullable=false) public boolean receiverApproved;
 @Column(length=20,nullable=false) public String status="pending";
 @Column(name="received_at") public Long receivedAt;
 @Column(name="created_at",nullable=false) public long createdAt=System.currentTimeMillis();
 @Column(name="updated_at",nullable=false) public long updatedAt=System.currentTimeMillis();
 protected BloodMatch(){}
}

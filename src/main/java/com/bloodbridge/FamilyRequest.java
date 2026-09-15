package com.bloodbridge;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="family_requests",indexes={@Index(name="idx_family_owner_status",columnList="owner_id,status")})
public class FamilyRequest {
 @Id @Column(length=36) public String id=UUID.randomUUID().toString();
 @Column(name="owner_id",length=36,nullable=false) public String ownerId;
 @Column(length=30,nullable=false) public String relationship;
 @Column(name="patient_name",length=80,nullable=false) public String patientName="";
 @Column(name="requester_name",length=80,nullable=false) public String requesterName="";
 @Column(length=180,nullable=false) public String email="";
 @Column(length=80,nullable=false) public String state="";
 @Column(length=120,nullable=false) public String hospital;
 @Column(length=80,nullable=false) public String city;
 @Column(name="blood_group",length=3,nullable=false) public String bloodGroup;
 @Column(length=10,nullable=false) public String phone;
 @Column(length=20,nullable=false) public String status="requested";
 @Column(length=500,nullable=false) public String note="";
 @Column(name="created_at",nullable=false) public long createdAt=System.currentTimeMillis();
 @Column(name="updated_at",nullable=false) public long updatedAt=System.currentTimeMillis();
 protected FamilyRequest(){}
}

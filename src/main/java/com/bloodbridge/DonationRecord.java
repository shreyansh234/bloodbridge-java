package com.bloodbridge;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="donation_records",uniqueConstraints={@UniqueConstraint(name="uq_donation_owner_date",columnNames={"owner_id","donation_date"})},indexes={@Index(name="idx_donation_owner_status",columnList="owner_id,status")})
public class DonationRecord {
 @Id @Column(length=36) public String id=UUID.randomUUID().toString();
 @Column(name="owner_id",length=36,nullable=false) public String ownerId;
 @Column(name="donation_date",length=10,nullable=false) public String donationDate;
 @Column(length=120,nullable=false) public String center;
 @Column(name="receipt_reference",length=100,nullable=false) public String reference;
 @Column(name="reference_key",length=64,nullable=false,unique=true) public String referenceKey;
 @Column(length=20,nullable=false) public String status="pending";
 @Column(name="review_note",length=500,nullable=false) public String reviewNote="";
 @Column(name="reviewed_by",length=36) public String reviewedBy;
 @Column(name="created_at",nullable=false) public long createdAt=System.currentTimeMillis();
 @Column(name="updated_at",nullable=false) public long updatedAt=System.currentTimeMillis();
 protected DonationRecord(){}
}

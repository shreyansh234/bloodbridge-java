package com.bloodbridge;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="donors",indexes={@Index(name="idx_donors_group_available",columnList="blood_group,available"),@Index(name="idx_donors_available_updated",columnList="available,updated_at")})
public class Donor {
 @Id @Column(length=36) public String id=UUID.randomUUID().toString();
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="account_id",nullable=false,unique=true) public Account account;
 @Column(nullable=false,length=80) public String name;
 @Column(name="blood_group",nullable=false,length=3) public String bloodGroup;
 @Column(nullable=false,length=80) public String city;
 @Column(nullable=false,length=80) public String area="";
 @Column(nullable=false,length=10) public String phone;
 @Column(nullable=false) public boolean available=true;
 @Column(nullable=false) public boolean consent=true;
 @Column(name="created_at",nullable=false) public long createdAt=System.currentTimeMillis();
 @Column(name="updated_at",nullable=false) public long updatedAt=System.currentTimeMillis();
 protected Donor() {}
}

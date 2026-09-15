package com.bloodbridge;
import jakarta.persistence.*;
@Entity @Table(name="device_access")
public class DeviceAccess {
 @Id @Column(name="token_hash",length=64) public String tokenHash;
 @Column(name="account_id",length=36,nullable=false) public String accountId;
 @Column(name="expires_at",nullable=false) public long expiresAt;
 protected DeviceAccess(){}
}

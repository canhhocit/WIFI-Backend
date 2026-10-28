package com.wificaptive.devices.entity;

import com.wificaptive.locations.entity.WifiLocation;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "device_swap_logs")
public class DeviceSwapLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private WifiLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_device_id")
    private NetworkDevice oldDevice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_device_id", nullable = false)
    private NetworkDevice newDevice;

    @Column(name = "old_serial_number", length = 100)
    private String oldSerialNumber;

    @Column(name = "new_serial_number", nullable = false, length = 100)
    private String newSerialNumber;

    @Column(name = "old_mac_address", length = 17)
    private String oldMacAddress;

    @Column(name = "new_mac_address", nullable = false, length = 17)
    private String newMacAddress;

    @Column(name = "swap_reason", nullable = false, length = 255)
    private String swapReason;

    @Column(name = "swapped_by", nullable = false, length = 50)
    private String swappedBy;

    @Column(name = "swapped_at", nullable = false)
    private Instant swappedAt = Instant.now();

    public DeviceSwapLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public WifiLocation getLocation() { return location; }
    public void setLocation(WifiLocation location) { this.location = location; }

    public NetworkDevice getOldDevice() { return oldDevice; }
    public void setOldDevice(NetworkDevice oldDevice) { this.oldDevice = oldDevice; }

    public NetworkDevice getNewDevice() { return newDevice; }
    public void setNewDevice(NetworkDevice newDevice) { this.newDevice = newDevice; }

    public String getOldSerialNumber() { return oldSerialNumber; }
    public void setOldSerialNumber(String oldSerialNumber) { this.oldSerialNumber = oldSerialNumber; }

    public String getNewSerialNumber() { return newSerialNumber; }
    public void setNewSerialNumber(String newSerialNumber) { this.newSerialNumber = newSerialNumber; }

    public String getOldMacAddress() { return oldMacAddress; }
    public void setOldMacAddress(String oldMacAddress) { this.oldMacAddress = oldMacAddress; }

    public String getNewMacAddress() { return newMacAddress; }
    public void setNewMacAddress(String newMacAddress) { this.newMacAddress = newMacAddress; }

    public String getSwapReason() { return swapReason; }
    public void setSwapReason(String swapReason) { this.swapReason = swapReason; }

    public String getSwappedBy() { return swappedBy; }
    public void setSwappedBy(String swappedBy) { this.swappedBy = swappedBy; }

    public Instant getSwappedAt() { return swappedAt; }
    public void setSwappedAt(Instant swappedAt) { this.swappedAt = swappedAt; }
}


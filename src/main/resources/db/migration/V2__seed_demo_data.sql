-- ==============================================================================
-- V2: Seed Demo Wi-Fi Locations and Baseline Configuration
-- ==============================================================================

-- 1. Realistic Public Wi-Fi Locations
INSERT INTO wifi_locations (
    slug, name, address, description, session_duration_minutes, max_bandwidth_mbps,
    terms_and_conditions, welcome_message, is_active, created_at, updated_at
) VALUES (
    'circle-k-central',
    'Circle K - Central Station',
    '102 Nguyen Hue Boulevard, District 1, Ho Chi Minh City',
    'High-traffic convenience store & express dining hub.',
    120,
    25,
    'Terms & Conditions for Circle K Public Wi-Fi: By connecting to this network, you agree not to use it for illegal, harassing, or harmful activities. Bandwidth is monitored to ensure fair service for all guests. Sessions automatically disconnect after 120 minutes. Internet access is provided as a complimentary convenience without performance guarantees.',
    'Welcome to Circle K Central Station High-Speed Wi-Fi',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'circle-k-airport',
    'Circle K - Airport Gateway Terminal',
    'Terminal 2 International Arrivals, Tan Son Nhat Airport',
    '24/7 express transit location with high concurrent traveler volume.',
    90,
    30,
    'Terms & Conditions for Circle K Airport Gateway: Complimentary Wi-Fi for transit travelers. You agree to adhere to acceptable use policies. Network activity is logged for security and capacity management.',
    'Welcome to Circle K Airport Gateway Wi-Fi',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'circle-k-riverside',
    'Circle K - Riverside Promenade',
    '45 Ton Duc Thang Street, Ben Nghe Ward',
    'Scenic outdoor seating area with mobile device priority.',
    60,
    15,
    'Acceptable Use Policy: Wi-Fi service is provided for customer personal communication and browsing. Commercial broadcasting and high-bandwidth P2P transfers are restricted.',
    'Welcome to Circle K Riverside Wi-Fi',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 2. Seed Devices
INSERT INTO devices (
    mac_address, ip_address, hostname, user_agent, os_type, first_seen_at, last_seen_at, is_blocked
) VALUES (
    'a4:83:e7:2b:41:90',
    '192.168.100.101',
    'iPhone-Guest',
    'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148',
    'iOS',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
),
(
    '5c:f9:38:91:a2:14',
    '192.168.100.102',
    'Galaxy-S24',
    'Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36',
    'Android',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
),
(
    '88:66:5a:33:cb:7e',
    '192.168.100.103',
    'MacBookPro-Guest',
    'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36',
    'macOS',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
);

-- 3. Seed Existing Historical & Active Guest Sessions
INSERT INTO guest_sessions (
    session_token, location_id, device_id, mac_address, ip_address,
    guest_name, guest_contact, consent_given, consent_timestamp,
    status, authorized_at, expires_at, gateway_reference,
    bytes_downloaded, bytes_uploaded, created_at, updated_at
) VALUES (
    'sess_demo_authorized_001',
    1,
    1,
    'a4:83:e7:2b:41:90',
    '192.168.100.101',
    'Alex Morgan',
    'alex.m@example.com',
    TRUE,
    CURRENT_TIMESTAMP,
    'AUTHORIZED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '120' MINUTE,
    'MOCK-GW-AUTH-1001',
    45829120,
    12492000,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'sess_demo_authorized_002',
    1,
    2,
    '5c:f9:38:91:a2:14',
    '192.168.100.102',
    'Thanh Nguyen',
    '+84901234567',
    TRUE,
    CURRENT_TIMESTAMP,
    'AUTHORIZED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '90' MINUTE,
    'MOCK-GW-AUTH-1002',
    89123440,
    18320490,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'sess_demo_revoked_003',
    2,
    3,
    '88:66:5a:33:cb:7e',
    '192.168.100.103',
    'Guest User 3',
    'guest3@example.com',
    TRUE,
    CURRENT_TIMESTAMP - INTERVAL '4' HOUR,
    'REVOKED',
    CURRENT_TIMESTAMP - INTERVAL '4' HOUR,
    CURRENT_TIMESTAMP - INTERVAL '2' HOUR,
    'MOCK-GW-AUTH-1003',
    12400000,
    2300000,
    CURRENT_TIMESTAMP - INTERVAL '4' HOUR,
    CURRENT_TIMESTAMP - INTERVAL '3' HOUR
);

-- 4. Initial Audit Events
INSERT INTO audit_events (
    action, actor_username, client_ip, target_type, target_id, details, created_at
) VALUES (
    'SYSTEM_BOOTSTRAP',
    'system',
    '127.0.0.1',
    'DATABASE',
    'INIT',
    'Database initialized with V2 baseline seed configuration.',
    CURRENT_TIMESTAMP
);

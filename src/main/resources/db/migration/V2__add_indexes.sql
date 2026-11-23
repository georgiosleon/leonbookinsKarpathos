-- Add indexes for common booking, resource, and user lookups.

CREATE INDEX IF NOT EXISTS idx_booking_room_status ON Booking (room, status);
CREATE INDEX IF NOT EXISTS idx_booking_start_date ON Booking (start_date);
CREATE INDEX IF NOT EXISTS idx_booking_user_id ON Booking (user_id);
CREATE INDEX IF NOT EXISTS idx_booking_resources_type_name ON booking_resources (resource_type, name);
CREATE INDEX IF NOT EXISTS idx_users_role_active ON users (role, is_active);

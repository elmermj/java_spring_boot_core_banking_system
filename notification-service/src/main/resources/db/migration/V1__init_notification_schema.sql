-- Create device_push_tokens table
CREATE TABLE device_push_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    push_token TEXT NOT NULL,
    platform VARCHAR(20) NOT NULL, -- ANDROID, IOS
    push_service VARCHAR(10) NOT NULL, -- FCM, APNS
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(device_id, user_id, platform)
);

-- Create indexes
CREATE INDEX idx_device_push_tokens_device_id ON device_push_tokens(device_id);
CREATE INDEX idx_device_push_tokens_user_id ON device_push_tokens(user_id);
CREATE INDEX idx_device_push_tokens_active ON device_push_tokens(is_active);

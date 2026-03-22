-- ClosetConnect Database Schema
-- Based on your ERD design

-- Create Users table
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    customer_name VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- Increased length for hashed passwords
    address VARCHAR(100),
    profile_image BYTEA, -- Changed from IMAGE to BYTEA for PostgreSQL
    rating INTEGER NOT NULL DEFAULT 0,
    bio VARCHAR(5000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create ClothingCard table
CREATE TABLE IF NOT EXISTS clothing_cards (
    clothing_id SERIAL PRIMARY KEY,
    owner_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    cost INTEGER NOT NULL,
    deposit INTEGER NOT NULL,
    size VARCHAR(5) NOT NULL,
    availability BOOLEAN NOT NULL DEFAULT true,
    date_posted TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description TEXT NOT NULL, -- Changed from TIMESTAMP to TEXT
    image BYTEA NOT NULL, -- Changed from IMAGE to BYTEA
    availability_day TIMESTAMP NOT NULL, -- Changed from availabilityDay
    rating INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create RentedClothingCard table
CREATE TABLE IF NOT EXISTS rented_clothing_cards (
    rental_id SERIAL PRIMARY KEY,
    clothing_id INTEGER NOT NULL REFERENCES clothing_cards(clothing_id) ON DELETE CASCADE,
    renter_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    rental_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    return_date TIMESTAMP NOT NULL,
    returned BOOLEAN DEFAULT false,
    request BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create UserReviews table
CREATE TABLE IF NOT EXISTS user_reviews (
    review_id SERIAL PRIMARY KEY,
    clothing_id INTEGER NOT NULL REFERENCES clothing_cards(clothing_id) ON DELETE CASCADE,
    reviewer_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    review INTEGER NOT NULL CHECK (review >= 1 AND review <= 5), -- Rating 1-5
    review_text TEXT, -- Added text field for actual review content
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create ClothingReviews table
CREATE TABLE IF NOT EXISTS clothing_reviews (
    review_id SERIAL PRIMARY KEY,
    clothing_id INTEGER NOT NULL REFERENCES clothing_cards(clothing_id) ON DELETE CASCADE,
    reviewer_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    review INTEGER NOT NULL CHECK (review >= 1 AND review <= 5), -- Rating 1-5
    review_text TEXT, -- Added text field for actual review content
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_clothing_cards_owner_id ON clothing_cards(owner_id);
CREATE INDEX IF NOT EXISTS idx_clothing_cards_availability ON clothing_cards(availability);
CREATE INDEX IF NOT EXISTS idx_rented_clothing_cards_clothing_id ON rented_clothing_cards(clothing_id);
CREATE INDEX IF NOT EXISTS idx_rented_clothing_cards_renter_id ON rented_clothing_cards(renter_id);
CREATE INDEX IF NOT EXISTS idx_user_reviews_clothing_id ON user_reviews(clothing_id);
CREATE INDEX IF NOT EXISTS idx_user_reviews_reviewer_id ON user_reviews(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_clothing_reviews_clothing_id ON clothing_reviews(clothing_id);
CREATE INDEX IF NOT EXISTS idx_clothing_reviews_reviewer_id ON clothing_reviews(reviewer_id);

-- Create triggers to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply triggers to all tables
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_clothing_cards_updated_at BEFORE UPDATE ON clothing_cards FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_rented_clothing_cards_updated_at BEFORE UPDATE ON rented_clothing_cards FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_reviews_updated_at BEFORE UPDATE ON user_reviews FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_clothing_reviews_updated_at BEFORE UPDATE ON clothing_reviews FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert sample data for testing
INSERT INTO users (customer_name, email, password, address, rating, bio) VALUES
('John Doe', 'john@example.com', 'hashed_password_123', '123 Main St, Sydney', 5, 'Fashion enthusiast with great style!'),
('Jane Smith', 'jane@example.com', 'hashed_password_456', '456 Oak Ave, Melbourne', 4, 'Love sharing my wardrobe with others.'),
('Mike Johnson', 'mike@example.com', 'hashed_password_789', '789 Pine Rd, Brisbane', 3, 'New to the platform, excited to start!');

-- Insert sample clothing items
INSERT INTO clothing_cards (owner_id, cost, deposit, size, availability, description, image, availability_day, rating) VALUES
(1, 25, 100, 'M', true, 'Elegant black dress perfect for formal events', E'\\x89504e470d0a1a0a', '2024-12-25 18:00:00', 5),
(2, 15, 75, 'S', true, 'Casual summer dress, great for beach days', E'\\x89504e470d0a1a0a', '2024-12-26 19:00:00', 4),
(3, 30, 120, 'L', true, 'Professional business suit for interviews', E'\\x89504e470d0a1a0a', '2024-12-27 20:00:00', 5);

-- Log successful table creation
DO $$
BEGIN
    RAISE NOTICE 'ClosetConnect database schema created successfully with 5 tables!';
    RAISE NOTICE 'Tables: users, clothing_cards, rented_clothing_cards, user_reviews, clothing_reviews';
    RAISE NOTICE 'Sample data inserted for testing';
END $$;

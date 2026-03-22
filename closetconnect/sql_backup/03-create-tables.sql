-- ClosetConnect Database Schema
-- Consistent camelCase naming throughout

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    userId SERIAL PRIMARY KEY,
    customerName VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    address VARCHAR(100),
    profileImage BYTEA,
    rating INTEGER NOT NULL DEFAULT 0,
    bio TEXT,
    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create clothingCards table
CREATE TABLE IF NOT EXISTS clothingCards (
    clothingId SERIAL PRIMARY KEY,
    ownerId INTEGER NOT NULL, -- Foreign key reference to users table
    cost INTEGER NOT NULL,
    deposit INTEGER NOT NULL,
    size VARCHAR(10) NOT NULL,
    availability BOOLEAN NOT NULL,
    datePosted TIMESTAMP NOT NULL,
    description TEXT,
    image BYTEA,
    availabilityDay TIMESTAMP NOT NULL,
    rating INTEGER NOT NULL DEFAULT 0,
    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create rentedClothingCards table
CREATE TABLE IF NOT EXISTS rentedClothingCards (
    rentalId SERIAL PRIMARY KEY,
    clothingCardId INTEGER NOT NULL,
    renterId INTEGER NOT NULL,
    rentalDate TIMESTAMP NOT NULL,
    returnDate TIMESTAMP NOT NULL,
    returned BOOLEAN DEFAULT FALSE,
    request BOOLEAN NOT NULL DEFAULT FALSE,
    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create clothingReviews table
CREATE TABLE IF NOT EXISTS clothingReviews (
    reviewId SERIAL PRIMARY KEY,
    clothingId INTEGER NOT NULL,
    reviewerId INTEGER NOT NULL,
    review TEXT NOT NULL,
    stars INTEGER NOT NULL,
    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create userReviews table
CREATE TABLE IF NOT EXISTS userReviews (
    reviewId SERIAL PRIMARY KEY,
    userId INTEGER NOT NULL,
    reviewerId INTEGER NOT NULL,
    review TEXT NOT NULL,
    stars INTEGER NOT NULL,
    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create wishlist table
CREATE TABLE IF NOT EXISTS wishlist (
    userId INTEGER NOT NULL,
    clothingId INTEGER NOT NULL,
    dateAdded TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (userId, clothingId)
);

-- Create foreign key constraints
ALTER TABLE clothingCards ADD CONSTRAINT fk_clothing_cards_owner 
    FOREIGN KEY (ownerId) REFERENCES users(userId) ON DELETE CASCADE;

ALTER TABLE rentedClothingCards ADD CONSTRAINT fk_rented_clothing_cards_clothing 
    FOREIGN KEY (clothingCardId) REFERENCES clothingCards(clothingId) ON DELETE CASCADE;

ALTER TABLE rentedClothingCards ADD CONSTRAINT fk_rented_clothing_cards_renter 
    FOREIGN KEY (renterId) REFERENCES users(userId) ON DELETE CASCADE;

ALTER TABLE clothingReviews ADD CONSTRAINT fk_clothing_reviews_clothing 
    FOREIGN KEY (clothingId) REFERENCES clothingCards(clothingId) ON DELETE CASCADE;

ALTER TABLE clothingReviews ADD CONSTRAINT fk_clothing_reviews_reviewer 
    FOREIGN KEY (reviewerId) REFERENCES users(userId) ON DELETE CASCADE;

ALTER TABLE userReviews ADD CONSTRAINT fk_user_reviews_user 
    FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE;

ALTER TABLE userReviews ADD CONSTRAINT fk_user_reviews_reviewer 
    FOREIGN KEY (reviewerId) REFERENCES users(userId) ON DELETE CASCADE;

ALTER TABLE wishlist ADD CONSTRAINT fk_wishlist_user 
    FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE;

ALTER TABLE wishlist ADD CONSTRAINT fk_wishlist_clothing 
    FOREIGN KEY (clothingId) REFERENCES clothingCards(clothingId) ON DELETE CASCADE;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_clothing_cards_owner_id ON clothingCards(ownerId);
CREATE INDEX IF NOT EXISTS idx_clothing_cards_availability ON clothingCards(availability);
CREATE INDEX IF NOT EXISTS idx_rented_clothing_cards_clothing_card_id ON rentedClothingCards(clothingCardId);
CREATE INDEX IF NOT EXISTS idx_rented_clothing_cards_renter_id ON rentedClothingCards(renterId);
CREATE INDEX IF NOT EXISTS idx_user_reviews_user_id ON userReviews(userId);
CREATE INDEX IF NOT EXISTS idx_user_reviews_reviewer_id ON userReviews(reviewerId);
CREATE INDEX IF NOT EXISTS idx_clothing_reviews_clothing_id ON clothingReviews(clothingId);
CREATE INDEX IF NOT EXISTS idx_clothing_reviews_reviewer_id ON clothingReviews(reviewerId);
CREATE INDEX IF NOT EXISTS idx_wishlist_user_id ON wishlist(userId);
CREATE INDEX IF NOT EXISTS idx_wishlist_clothing_id ON wishlist(clothingId);

-- Insert sample users
INSERT INTO users (customerName, email, password, address, rating, bio) VALUES
('John Doe', 'john@example.com', '$2a$10$UPaYiQry8tf8i.Sw.BhtyeIASW3ZHhI9AuKlp6MjGVRi3E/I6LIn6', '123 Main St, Sydney', 5, 'Fashion enthusiast with great style!'),
('Jane Smith', 'jane@example.com', '$2a$10$UPaYiQry8tf8i.Sw.BhtyeIASW3ZHhI9AuKlp6MjGVRi3E/I6LIn6', '456 Oak Ave, Melbourne', 4, 'Love sharing my wardrobe with others.'),
('Mike Johnson', 'mike@example.com', '$2a$10$UPaYiQry8tf8i.Sw.BhtyeIASW3ZHhI9AuKlp6MjGVRi3E/I6LIn6', '789 Pine Rd, Brisbane', 3, 'New to the platform, excited to start!');

-- Insert sample clothing cards
INSERT INTO clothingCards (ownerId, cost, deposit, size, availability, datePosted, description, image, availabilityDay, rating) VALUES
(1, 15, 50, 'M', TRUE, '2025-09-01 10:00:00', 'Red vintage dress, perfect for parties.', NULL, '2025-09-05 00:00:00', 5),
(2, 10, 30, 'L', TRUE, '2025-09-02 14:30:00', 'Oversized hoodie, super comfy.', NULL, '2025-09-06 00:00:00', 4),
(3, 20, 60, 'S', FALSE, '2025-08-30 09:15:00', 'White linen shirt, breathable and light.', NULL, '2025-09-10 00:00:00', 3);

-- Create triggers to automatically update updatedAt timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updatedAt = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply triggers to all tables
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_clothing_cards_updated_at BEFORE UPDATE ON clothingCards FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_rented_clothing_cards_updated_at BEFORE UPDATE ON rentedClothingCards FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_user_reviews_updated_at BEFORE UPDATE ON userReviews FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_clothing_reviews_updated_at BEFORE UPDATE ON clothingReviews FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Log successful table creation
DO $$
BEGIN
    RAISE NOTICE 'ClosetConnect database schema created successfully with 6 tables!';
    RAISE NOTICE 'Tables: users, clothingCards, rentedClothingCards, userReviews, clothingReviews, wishlist';
    RAISE NOTICE 'Sample data inserted for testing';
END $$;
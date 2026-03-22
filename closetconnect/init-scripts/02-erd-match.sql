CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    address VARCHAR(100),
    profile_image BYTEA,
    rating INTEGER NOT NULL DEFAULT 0,
    bio VARCHAR(5000),
    verification_status BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS clothing_cards (
    clothing_id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    cost INTEGER NOT NULL,
    deposit INTEGER NOT NULL,
    size VARCHAR(5) NOT NULL,
    availability BOOLEAN NOT NULL DEFAULT TRUE,
    date_posted TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description TEXT NOT NULL,
    availability_day TIMESTAMP NOT NULL,
    rating INTEGER NOT NULL DEFAULT 0,
    tag TEXT,
    brand TEXT,
    location_of_clothing TEXT,
    gender boolean DEFAULT FALSE, -- TRUE for male, False female
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS clothing_images (
    image_id BIGSERIAL PRIMARY KEY,
    clothing_id BIGINT NOT NULL REFERENCES clothing_cards(clothing_id) ON DELETE CASCADE,
    image BYTEA NOT NULL,
    position INTEGER NOT NULL DEFAULT 0 CHECK (position >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (clothing_id, position)
);

ALTER TABLE clothing_cards
    ADD COLUMN IF NOT EXISTS cover_image_id BIGINT NULL;

ALTER TABLE clothing_cards
    ADD CONSTRAINT fk_cover_image
        FOREIGN KEY (cover_image_id)
        REFERENCES clothing_images(image_id)
        ON DELETE SET NULL
        DEFERRABLE INITIALLY DEFERRED;

-- RENTALS
CREATE TABLE IF NOT EXISTS rented_clothing_cards (
    rental_id BIGSERIAL PRIMARY KEY,
    clothing_id BIGINT NOT NULL REFERENCES clothing_cards(clothing_id) ON DELETE CASCADE,
    renter_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    rental_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    return_date TIMESTAMP NOT NULL,
    returned BOOLEAN NOT NULL DEFAULT FALSE,
    request BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (return_date > rental_date)
);

-- REVIEWS (for clothing items)
CREATE TABLE IF NOT EXISTS clothing_reviews (
    review_id BIGSERIAL PRIMARY KEY,
    clothing_id BIGINT NOT NULL REFERENCES clothing_cards(clothing_id) ON DELETE CASCADE,
    reviewer_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    review INTEGER NOT NULL CHECK (review BETWEEN 1 AND 5),
    review_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (clothing_id, reviewer_id)
);

-- REQUESTS (consider using tsrange/daterange later if you want ranges)
CREATE TABLE IF NOT EXISTS requests (
    request_id BIGSERIAL PRIMARY KEY,
    clothing_id BIGINT NOT NULL REFERENCES clothing_cards(clothing_id) ON DELETE CASCADE,
    from_user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    to_user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    availability_range_request TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- USER REVIEWS (reviewers reviewing other users for a rental transaction)
-- Supports both directions: owner reviewing renter, and renter reviewing owner
CREATE TABLE IF NOT EXISTS user_reviews (
    review_id BIGSERIAL PRIMARY KEY,
    rental_id BIGINT NOT NULL REFERENCES rented_clothing_cards(rental_id) ON DELETE CASCADE,
    reviewed_user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE, -- The user being reviewed
    reviewer_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,      -- The user writing the review
    review INTEGER NOT NULL CHECK (review BETWEEN 1 AND 5),
    review_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (rental_id, reviewer_id), -- One review per reviewer per rental transaction
    CHECK (reviewed_user_id != reviewer_id) -- Cannot review yourself
);

CREATE TABLE IF NOT EXISTS wishlist (
  user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  clothing_id BIGINT NOT NULL REFERENCES clothing_cards(clothing_id) ON DELETE CASCADE,
  date_added TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, clothing_id)
);


-- INDEXES
CREATE INDEX IF NOT EXISTS idx_clothing_cards_owner_id ON clothing_cards(owner_id);
CREATE INDEX IF NOT EXISTS idx_clothing_cards_availability ON clothing_cards(availability);

CREATE INDEX IF NOT EXISTS idx_clothing_images_clothing_id ON clothing_images(clothing_id);
CREATE INDEX IF NOT EXISTS idx_clothing_images_order ON clothing_images(clothing_id, position);

CREATE INDEX IF NOT EXISTS idx_rented_clothing_cards_clothing_id ON rented_clothing_cards(clothing_id);
CREATE INDEX IF NOT EXISTS idx_rented_clothing_cards_renter_id ON rented_clothing_cards(renter_id);

CREATE INDEX IF NOT EXISTS idx_user_reviews_rental_id ON user_reviews(rental_id);
CREATE INDEX IF NOT EXISTS idx_user_reviews_reviewed_user_id ON user_reviews(reviewed_user_id);
CREATE INDEX IF NOT EXISTS idx_user_reviews_reviewer_id ON user_reviews(reviewer_id);

CREATE INDEX IF NOT EXISTS idx_clothing_reviews_clothing_id ON clothing_reviews(clothing_id);
CREATE INDEX IF NOT EXISTS idx_clothing_reviews_reviewer_id ON clothing_reviews(reviewer_id);

CREATE INDEX IF NOT EXISTS idx_requests_clothing_id ON requests(clothing_id);
CREATE INDEX IF NOT EXISTS idx_requests_from_user_id ON requests(from_user_id);
CREATE INDEX IF NOT EXISTS idx_requests_to_user_id ON requests(to_user_id);

-- UPDATED_AT trigger fn
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers
CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_clothing_cards_updated_at
BEFORE UPDATE ON clothing_cards
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_clothing_images_updated_at
BEFORE UPDATE ON clothing_images
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_rented_clothing_cards_updated_at
BEFORE UPDATE ON rented_clothing_cards
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_user_reviews_updated_at
BEFORE UPDATE ON user_reviews
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_clothing_reviews_updated_at
BEFORE UPDATE ON clothing_reviews
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_requests_updated_at
BEFORE UPDATE ON requests
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Optional: a constraint to ensure cover_image_id (if set) belongs to this clothing_id
-- (enforced via trigger because it's a cross-table check)
CREATE OR REPLACE FUNCTION ensure_cover_image_matches_card()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.cover_image_id IS NOT NULL THEN
        PERFORM 1
        FROM clothing_images ci
        WHERE ci.image_id = NEW.cover_image_id
          AND ci.clothing_id = NEW.clothing_id;
        IF NOT FOUND THEN
            RAISE EXCEPTION 'cover_image_id % does not belong to clothing_id %', NEW.cover_image_id, NEW.clothing_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_cover_image_matches_card ON clothing_cards;
CREATE CONSTRAINT TRIGGER trg_cover_image_matches_card
AFTER INSERT OR UPDATE OF cover_image_id, clothing_id ON clothing_cards
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION ensure_cover_image_matches_card();


CREATE INDEX IF NOT EXISTS idx_wishlist_user_id ON wishlist(user_id);
CREATE INDEX IF NOT EXISTS idx_wishlist_clothing_id ON wishlist(clothing_id);

-- Seed users
INSERT INTO users (user_name, email, password, address, rating, bio, verification_status)
VALUES
('Admin', 'admin@gmail.com', crypt('admin', gen_salt('bf')), NULL, 0, 'Administrator account', TRUE),
('John Doe', 'john@example.com', crypt('password123', gen_salt('bf')), '123 Main St, Sydney', 5, 'Fashion enthusiast with great style!', FALSE),
('Jane Smith', 'jane@example.com', crypt('password456', gen_salt('bf')), '456 Oak Ave, Melbourne', 4, 'Love sharing my wardrobe with others.', FALSE),
('Mike Johnson', 'mike@example.com', crypt('password789', gen_salt('bf')), '789 Pine Rd, Brisbane', 3, 'New to the platform, excited to start!', FALSE)
ON CONFLICT (email) DO NOTHING;

DO $$
BEGIN
    RAISE NOTICE 'ClosetConnect schema created/updated with clothing_images and cover_image_id integrity.';
END $$;

CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       username VARCHAR(100) UNIQUE NOT NULL,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       first_name VARCHAR(100),
                       last_name VARCHAR(100),
                       role VARCHAR(50) DEFAULT 'CUSTOMER',
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       last_login TIMESTAMP WITH TIME ZONE
);

CREATE TABLE media_assets (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              uploader_id UUID REFERENCES users(id),
                              type VARCHAR(50) NOT NULL,
                              status VARCHAR(50) DEFAULT 'PROCESSING',

                              bucket_name VARCHAR(100) NOT NULL,
                              file_key VARCHAR(512) NOT NULL,
                              cdn_url VARCHAR(512) NOT NULL,

                              file_size_bytes BIGINT,
                              mime_type VARCHAR(50),
                              resolution_width INT,
                              resolution_height INT,
                              duration_seconds INT,

                              camera_model VARCHAR(100),
                              lens_info VARCHAR(100),

                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cards (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       author_id UUID REFERENCES users(id),
                       status VARCHAR(50) DEFAULT 'DRAFT',

                       title VARCHAR(255) NOT NULL,
                       description TEXT,

                       species_common_name VARCHAR(255),
                       species_scientific_name VARCHAR(255),
                       conservation_status VARCHAR(50),

                       location_name VARCHAR(255),
                       latitude DECIMAL(10, 8),
                       longitude DECIMAL(11, 8),

                       primary_media_id UUID REFERENCES media_assets(id),
                       thumbnail_media_id UUID REFERENCES media_assets(id),

                       is_premium BOOLEAN DEFAULT FALSE,
                       price DECIMAL(10, 2) DEFAULT 0.00,

                       view_count BIGINT DEFAULT 0,
                       favorite_count BIGINT DEFAULT 0,

                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            parent_id UUID REFERENCES categories(id),
                            slug VARCHAR(100) UNIQUE NOT NULL,
                            name VARCHAR(100) NOT NULL,
                            description TEXT
);

CREATE TABLE tags (
                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                      slug VARCHAR(50) UNIQUE NOT NULL,
                      name VARCHAR(50) NOT NULL
);

CREATE TABLE card_categories (
                                 card_id UUID REFERENCES cards(id) ON DELETE CASCADE,
                                 category_id UUID REFERENCES categories(id) ON DELETE CASCADE,
                                 PRIMARY KEY (card_id, category_id)
);

CREATE TABLE card_tags (
                           card_id UUID REFERENCES cards(id) ON DELETE CASCADE,
                           tag_id UUID REFERENCES tags(id) ON DELETE CASCADE,
                           PRIMARY KEY (card_id, tag_id)
);

CREATE TABLE user_favorites (
                                user_id UUID REFERENCES users(id) ON DELETE CASCADE,
                                card_id UUID REFERENCES cards(id) ON DELETE CASCADE,
                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (user_id, card_id)
);


CREATE TABLE event_publication (
                                   id UUID PRIMARY KEY,
                                   listener_id VARCHAR(512) NOT NULL,
                                   event_type VARCHAR(512) NOT NULL,
                                   serialized_event VARCHAR(4000) NOT NULL,
                                   publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
                                   completion_date TIMESTAMP WITH TIME ZONE,
                                   completion_attempts INTEGER,
                                   last_resubmission_date TIMESTAMP WITH TIME ZONE,
                                   status VARCHAR(255) -- Add this column
);

CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication (event_type, listener_id);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);
CREATE INDEX idx_cards_status ON cards(status);
CREATE INDEX idx_cards_species ON cards(species_scientific_name);
CREATE INDEX idx_cards_location ON cards(location_name);
CREATE INDEX idx_user_favorites_user_id ON user_favorites(user_id);
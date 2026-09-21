CREATE TABLE uploaded_files (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_name VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(80) NOT NULL,
    file_size BIGINT NOT NULL,
    public_url VARCHAR(500) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_uploaded_files_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE post_images (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT uk_post_images_post_file UNIQUE (post_id, file_id),
    CONSTRAINT fk_post_images_post FOREIGN KEY (post_id) REFERENCES posts(id),
    CONSTRAINT fk_post_images_file FOREIGN KEY (file_id) REFERENCES uploaded_files(id)
);

CREATE TABLE reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    handled_by BIGINT,
    handle_note VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    handled_at TIMESTAMP,
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_reports_handler FOREIGN KEY (handled_by) REFERENCES users(id)
);

CREATE INDEX idx_uploaded_files_user_created ON uploaded_files(user_id, created_at);
CREATE INDEX idx_post_images_post_sort ON post_images(post_id, sort_order);
CREATE INDEX idx_reports_status_created ON reports(status, created_at);


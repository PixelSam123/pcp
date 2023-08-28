-- This file allow to write SQL commands that will be emitted in test and dev.
-- The commands are commented as their support depends of the database
-- insert into myentity (id, field) values(1, 'field-1');
-- insert into myentity (id, field) values(2, 'field-2');
-- insert into myentity (id, field) values(3, 'field-3');
-- alter sequence myentity_seq restart with 4;

CREATE TABLE IF NOT EXISTS user
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    name          VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(255) DEFAULT 'User',
    points        INT          DEFAULT 0
);

CREATE TABLE IF NOT EXISTS challenge
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(255) UNIQUE,
    description     VARCHAR(255) NOT NULL,
    initial_code    VARCHAR(255) NOT NULL,
    test_case       VARCHAR(255) NOT NULL,
    tier            INT          NOT NULL,
    completed_count INT DEFAULT 0,
    user_id         BIGINT,
    CONSTRAINT fk_user_challenge
        FOREIGN KEY (user_id) REFERENCES user (id)
);

CREATE TABLE IF NOT EXISTS challenge_comment
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    content      VARCHAR(255) NOT NULL,
    user_id      BIGINT,
    challenge_id BIGINT,
    CONSTRAINT fk_user_challenge_comment
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk_challenge_challenge_comment
        FOREIGN KEY (challenge_id) REFERENCES challenge (id)
);

CREATE TABLE IF NOT EXISTS challenge_vote
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    is_upvote    BOOL NOT NULL,
    user_id      BIGINT,
    challenge_id BIGINT,
    CONSTRAINT fk_user_challenge_vote
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk_challenge_challenge_vote
        FOREIGN KEY (challenge_id) REFERENCES challenge (id)
);

CREATE TABLE IF NOT EXISTS challenge_submission
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    code         VARCHAR(255) NOT NULL,
    user_id      BIGINT,
    challenge_id BIGINT,
    CONSTRAINT fk_user_challenge_submission
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk_challenge_challenge_submission
        FOREIGN KEY (challenge_id) REFERENCES challenge (id)
);

CREATE TABLE IF NOT EXISTS challenge_submission_comment
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    content                 VARCHAR(255) NOT NULL,
    user_id                 BIGINT,
    challenge_submission_id BIGINT,
    CONSTRAINT fk_user_challenge_submission_comment
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk_challenge_submission_challenge_submission_comment
        FOREIGN KEY (challenge_submission_id) REFERENCES challenge_submission (id)
);

CREATE TABLE IF NOT EXISTS challenge_submission_vote
(
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    is_upvote               BOOL NOT NULL,
    user_id                 BIGINT,
    challenge_submission_id BIGINT,
    CONSTRAINT fk_user_challenge_submission_vote
        FOREIGN KEY (user_id) REFERENCES user (id),
    CONSTRAINT fk_challenge_submission_challenge_submission_vote
        FOREIGN KEY (challenge_submission_id) REFERENCES challenge_submission (id)
);
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id     BIGINT PRIMARY KEY,
    name   VARCHAR(64),
    age    INT,
    remark VARCHAR(255)
);
INSERT INTO t_user (id, name, age, remark) VALUES (1, 'alice', 20, 'a?b');
INSERT INTO t_user (id, name, age, remark) VALUES (2, 'bob', 30, 'plain');
INSERT INTO t_user (id, name, age, remark) VALUES (3, 'o''brien', 40, 'quote');
INSERT INTO t_user (id, name, age, remark) VALUES (4, 'carol', 50, 'plain');

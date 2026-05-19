SELECT id, username, nickname, CONCAT(LEFT(password, 30), '...') AS password_hash FROM `user` ORDER BY id;

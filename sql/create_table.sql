CREATE TABLE `bans` (
	`id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
	`player_id` VARCHAR(36) NOT NULL COLLATE 'utf8mb4_general_ci',
	`mod_id` VARCHAR(36) NULL DEFAULT NULL COLLATE 'utf8mb4_general_ci',
	`creation_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	`expiration_date` DATETIME NULL DEFAULT NULL,
	`reason` VARCHAR(64) NOT NULL DEFAULT '' COLLATE 'utf8mb4_general_ci',
	PRIMARY KEY (`id`) USING BTREE,
	INDEX `bans_mod_id` (`player_id`) USING BTREE,
	INDEX `bans_player_id` (`mod_id`) USING BTREE,
)
COLLATE='utf8mb4_general_ci'
ENGINE=InnoDB
;

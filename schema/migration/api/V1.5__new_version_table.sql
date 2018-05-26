CREATE TABLE `flow_v1` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `flow_status` varchar(20) NOT NULL,
  `envs` longtext,
  `created_by` varchar(100) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `flow_name_unique_key` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# ------------------------------------------------------------

CREATE TABLE `flow_yml_v1` (
  `flow_id`bigint(20) NOT NULL,
  `content` longblob,
  PRIMARY KEY (`flow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# ------------------------------------------------------------

CREATE TABLE `job_v1` (
  `flow_id` bigint(20) NOT NULL,
  `build_number` bigint(20) NOT NULL,
  `envs` longtext,
  `job_category` varchar(20) NOT NULL,
  `job_status` varchar(20) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `created_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`flow_id`, `build_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# ------------------------------------------------------------

CREATE TABLE `job_tree_v1` (
  `flow_id` bigint(20) NOT NULL,
  `build_number` bigint(20) NOT NULL,
  `tree` longblob NOT NULL,
  PRIMARY KEY (`flow_id`, `build_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# ------------------------------------------------------------

CREATE TABLE `job_number_v1` (
  `flow_id` bigint(20) NOT NULL,
  `build_number` bigint(20) NOT NULL,
  PRIMARY KEY (`flow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# ------------------------------------------------------------

CREATE TABLE `user_flow_v1` (
  `flow_id` bigint(20) NOT NULL,
  `user_email` varchar(100) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`flow_id`,`user_email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*
 Navicat Premium Data Transfer

 Source Server         : Local PostGIS
 Source Server Type    : PostgreSQL
 Source Server Version : 90203
 Source Host           : localhost
 Source Database       : lifestream
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 90203
 File Encoding         : utf-8

 Date: 04/02/2013 03:09:19 AM
*/

-- ----------------------------
--  Table structure for "user"
-- ----------------------------
DROP TABLE IF EXISTS "user";
CREATE TABLE "user" (
  "id"          UUID         NOT NULL PRIMARY KEY,
  "username"    VARCHAR(256) NOT NULL,
  "email"       VARCHAR(256) NOT NULL UNIQUE,
  "password"    VARCHAR(64)  NOT NULL,
  "created_ts"  TIMESTAMP(6) NOT NULL,
  "modified_ts" TIMESTAMP(6) NOT NULL
)
WITH (OIDS = FALSE);
ALTER TABLE "user" OWNER TO "lifestream";

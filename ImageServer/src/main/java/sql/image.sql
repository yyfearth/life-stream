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

 Date: 04/01/2013 16:18:55 PM
*/

-- ----------------------------
--  Table structure for "image"
-- ----------------------------
DROP TABLE IF EXISTS "image";
CREATE TABLE "image" (
  "id"           UUID         NOT NULL,
  "name"         VARCHAR(256) NOT NULL,
  "mime"         VARCHAR(256) NOT NULL,
  "length"       INT8         NOT NULL,
--"user_id"      UUID,
--"desc"         TEXT,
  "width"        INT4,
  "height"       INT4,
  "geo_location" geometry,
  "original_ts"  TIMESTAMP    NULL,
  "created_ts"   TIMESTAMP    NOT NULL,
  "modified_ts"  TIMESTAMP    NOT NULL
)
WITH (OIDS = FALSE);
ALTER TABLE "image" OWNER TO "lifestream";

-- ----------------------------
--  Primary key structure for table "image"
-- ----------------------------
ALTER TABLE "image" ADD CONSTRAINT "image_pkey" PRIMARY KEY ("id") NOT DEFERRABLE INITIALLY IMMEDIATE;


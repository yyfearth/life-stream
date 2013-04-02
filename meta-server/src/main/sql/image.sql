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
	"id" uuid NOT NULL,
	--"user_id" uuid NOT NULL,
	"name" varchar(256) NOT NULL,
	"mime" varchar(256) NOT NULL,
	"length" int8 NOT NULL,
	--"desc" text,
	"width" int4,
	"height" int4,
	"geo_location" geometry,
	"original_ts" timestamp NULL,
	"created_ts" timestamp NOT NULL,
	"modified_ts" timestamp NOT NULL
)
WITH (OIDS=FALSE);
ALTER TABLE "image" OWNER TO "wilson";

-- ----------------------------
--  Primary key structure for table "image"
-- ----------------------------
ALTER TABLE "image" ADD CONSTRAINT "image_pkey" PRIMARY KEY ("id") NOT DEFERRABLE INITIALLY IMMEDIATE;


-- Allow authors to recreate a category or tag name after soft deletion.
-- Generated active-name columns are NULL for deleted rows; MySQL unique
-- indexes allow multiple NULL values while still enforcing one active name.

ALTER TABLE `blog_category`
    DROP INDEX `uk_category_author_name`,
    ADD COLUMN `active_name` VARCHAR(50)
        GENERATED ALWAYS AS (
            CASE WHEN `is_deleted` = 0 THEN `name` ELSE NULL END
        ) STORED,
    ADD UNIQUE KEY `uk_category_author_active_name` (`author_id`, `active_name`);

ALTER TABLE `blog_tag`
    DROP INDEX `uk_tag_creator_name`,
    ADD COLUMN `active_name` VARCHAR(50)
        GENERATED ALWAYS AS (
            CASE WHEN `is_deleted` = 0 THEN `name` ELSE NULL END
        ) STORED,
    ADD UNIQUE KEY `uk_tag_creator_active_name` (`created_by`, `active_name`);

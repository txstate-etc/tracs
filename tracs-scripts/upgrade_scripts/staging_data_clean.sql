-- -----------------------------------------------------
--   This is for cleaning some useless data when cloning
--   from production.
-- -----------------------------------------------------

--  Remove cron triggers for all quartz jobs

TRUNCATE TABLE QRTZ_CRON_TRIGGERS;
DELETE FROM QRTZ_TRIGGERS
WHERE TRIGGER_TYPE='CRON';

--  Clearing out events from TRACS production

-- Looking for other alternatives to stop flooding notifications
-- during server startup on staging
-- TRUNCATE TABLE SAKAI_EVENT;
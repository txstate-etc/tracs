CREATE TABLE IF NOT EXISTS `SST_EVENT_DETAIL` (
  `ID` bigint(20) NOT NULL auto_increment,
  `USER_ID` varchar(99) NOT NULL,
  `SITE_ID` varchar(99) NOT NULL,
  `EVENT_ID` varchar(32) NOT NULL,
  `EVENT_DATE` datetime NOT NULL,
  `ITEM_TYPE` varchar(99) default NULL,
  `ITEM_ID` varchar(99) default NULL,
  PRIMARY KEY  (`ID`)
);
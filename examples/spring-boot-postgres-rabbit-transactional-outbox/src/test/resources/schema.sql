-- Taken from https://raw.githubusercontent.com/spring-projects/spring-integration/main/spring-integration-jdbc/src/main/resources/org/springframework/integration/jdbc/schema-postgresql.sql

CREATE TABLE ENTITY (
  ID UUID NOT NULL constraint ENTITY_PK primary key,
  VALUE VARCHAR(10) not null
);

CREATE SEQUENCE OUTBOX_MESSAGE_SEQ START WITH 1 INCREMENT BY 1 NO CYCLE;

CREATE TABLE OUTBOX_CHANNEL_MESSAGE (
                                     MESSAGE_ID CHAR(36) NOT NULL,
                                     GROUP_KEY CHAR(36) NOT NULL,
                                     CREATED_DATE BIGINT NOT NULL,
                                     MESSAGE_PRIORITY BIGINT,
                                     MESSAGE_SEQUENCE BIGINT NOT NULL DEFAULT nextval('OUTBOX_MESSAGE_SEQ'),
                                     MESSAGE_BYTES BYTEA,
                                     REGION VARCHAR(100) NOT NULL,
                                     constraint OUTBOX_CHANNEL_MESSAGE_PK primary key (REGION, GROUP_KEY, CREATED_DATE, MESSAGE_SEQUENCE)
);

CREATE INDEX OUTBOX_CHANNEL_MSG_DELETE_IDX ON OUTBOX_CHANNEL_MESSAGE (REGION, GROUP_KEY, MESSAGE_ID);

CREATE FUNCTION OUTBOX_CHANNEL_MESSAGE_NOTIFY_FCT()
RETURNS TRIGGER AS
 $BODY$
 BEGIN
     PERFORM pg_notify('outbox_channel_message_notify', NEW.REGION || ' ' || NEW.GROUP_KEY);
     RETURN NEW;
 END;
 $BODY$
 LANGUAGE PLPGSQL;

 CREATE TRIGGER OUTBOX_CHANNEL_MESSAGE_NOTIFY_TRG
 AFTER INSERT ON OUTBOX_CHANNEL_MESSAGE
 FOR EACH ROW
 EXECUTE FUNCTION OUTBOX_CHANNEL_MESSAGE_NOTIFY_FCT();

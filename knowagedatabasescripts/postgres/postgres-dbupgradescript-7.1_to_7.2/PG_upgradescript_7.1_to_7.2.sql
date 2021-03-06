--adding POPUP_OPTIONS into SBI_CROSS_NAVIGATION table
ALTER TABLE SBI_CROSS_NAVIGATION ADD COLUMN POPUP_OPTIONS VARCHAR(4000) NULL DEFAULT NULL;

--- START ---
-- 15/01/2020 Radmila Selakovic
ALTER TABLE SBI_CROSS_NAVIGATION ADD FROM_DOC_ID INTEGER DEFAULT NULL;
-- Create foreign key between SBI_CROSS_NAVIGATION and SBI_OBJECTS
ALTER TABLE SBI_CROSS_NAVIGATION ADD CONSTRAINT FK_SBI_CROSS_NAVIGATION_1 FOREIGN KEY (FROM_DOC_ID) REFERENCES SBI_OBJECTS(BIOBJ_ID);

ALTER TABLE SBI_CROSS_NAVIGATION ADD TO_DOC_ID INTEGER DEFAULT NULL;
-- Create foreign key between SBI_CROSS_NAVIGATION and SBI_OBJECTS
ALTER TABLE SBI_CROSS_NAVIGATION ADD CONSTRAINT FK_SBI_CROSS_NAVIGATION_2 FOREIGN KEY (TO_DOC_ID) REFERENCES SBI_OBJECTS(BIOBJ_ID);

-- 25/02/2020 Radmila Selakovic
ALTER TABLE SBI_MENU ADD ICON VARCHAR(255) NULL;
ALTER TABLE SBI_MENU ADD CUST_ICON VARCHAR(4000) NULL; 

--14/04/2020 Radmila Selakovic KNOWAGE-5009
UPDATE SBI_ALERT_LISTENER SET TEMPLATE='angular_1.4/tools/alert/listeners/kpiListener/templates/kpiListener.html' WHERE NAME='KPI Listener';
UPDATE SBI_ALERT_ACTION SET TEMPLATE='angular_1.4/tools/alert/actions/executeETL/templates/executeETL.html' WHERE NAME= 'Execute ETL Document';
UPDATE SBI_ALERT_ACTION SET TEMPLATE='angular_1.4/tools/alert/actions/sendMail/templates/sendMail.html' WHERE NAME= 'Send mail';
UPDATE SBI_ALERT_ACTION SET TEMPLATE='angular_1.4/tools/alert/actions/contextBroker/templates/contextBroker.html' WHERE NAME= 'Context Broker';

--08/06/2020 Andrijana Predojevic
ALTER TABLE SBI_OBJ_PARUSE ADD CONSTRAINT XAK1SBI_OBJ_PARUSE UNIQUE (OBJ_PAR_ID,USE_ID,OBJ_PAR_FATHER_ID,FILTER_OPERATION);

--09/06/2020 Andrijana Predojevic
ALTER TABLE SBI_METAMODEL_PARUSE ADD CONSTRAINT XAK1SBI_METAMODEL_PARUSE UNIQUE (METAMODEL_PAR_ID,USE_ID,METAMODEL_PAR_FATHER_ID,FILTER_OPERATION); 

ALTER TABLE SBI_OBJECTS ADD CONSTRAINT XAK2SBI_OBJECTS UNIQUE (NAME, ORGANIZATION);
ALTER TABLE SBI_LOV ADD CONSTRAINT XAK2SBI_LOV UNIQUE (NAME, ORGANIZATION);
ALTER TABLE SBI_PARAMETERS ADD CONSTRAINT XAK2SBI_PARAMETERS UNIQUE (NAME, ORGANIZATION);

--15/06/2020 Andrijana Predojevic
ALTER TABLE SBI_META_MODELS ADD COLUMN SMART_VIEW BOOLEAN DEFAULT TRUE;

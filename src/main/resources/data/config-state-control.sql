INSERT INTO public.statemachine_control (use_case_id,use_case_name,id_in_case,step,from_state,to_state,state_formula,subscribe_topic,publish_topic,api_call_config,creation_time,last_update_time) VALUES
	 (1,'tpsg-gmt-poc-propose',1,1,'init','getQuote',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.propose',NULL,'2024-09-12 23:07:37.141155','2024-09-12 23:07:37.141155'),
	 (1,'tpsg-gmt-poc-propose',2,2,'getQuoteDone','injectRefId',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.propose',NULL,'2024-09-12 23:07:37.193652','2024-09-12 23:07:37.193652'),
	 (1,'tpsg-gmt-poc-propose',3,3,'injectRefIdDone','globalLimitCheck',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.propose',NULL,'2024-09-12 23:07:37.245216','2024-09-12 23:07:37.245216'),
	 (1,'tpsg-gmt-poc-propose',4,4,'globalLimitChecked','validateDebitAccount',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.propose',NULL,'2024-09-12 23:07:37.295671','2024-09-12 23:07:37.295671'),
	 (1,'tpsg-gmt-poc-propose',5,5,'validatedDebitAccount','validateCreditAccount',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.propose',NULL,'2024-09-12 23:07:37.342921','2024-09-12 23:07:37.342921'),
	 (1,'tpsg-gmt-poc-propose',6,6,'validatedCreditAccount','fxPolicyPropose',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.propose',NULL,'2024-09-12 23:07:37.389189','2024-09-12 23:07:37.389189'),
	 (2,'tpsg-gmt-poc-confirm',1,1,'initConfirm','fxBookDeal',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.fxq',NULL,'2024-09-12 23:07:37.485418','2024-09-12 23:07:37.485418'),
	 (2,'tpsg-gmt-poc-confirm',2,2,'fxBookDealDone','globalLimitUpdate',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.glm',NULL,'2024-09-12 23:07:37.534967','2024-09-12 23:07:37.534967'),
	 (2,'tpsg-gmt-poc-confirm',3,3,'globalLimitUpdated','coolingOffCheck',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.cooloff',NULL,'2024-09-12 23:07:37.583885','2024-09-12 23:07:37.583885'),
	 (2,'tpsg-gmt-poc-confirm',4,4,'coolingOffChecked','fraudCheck',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.fraud',NULL,'2024-09-12 23:07:37.632114','2024-09-12 23:07:37.632114');
INSERT INTO public.statemachine_control (use_case_id,use_case_name,id_in_case,step,from_state,to_state,state_formula,subscribe_topic,publish_topic,api_call_config,creation_time,last_update_time) VALUES
	 (2,'tpsg-gmt-poc-confirm',5,5,'fraudChecked','sanctionCheck',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.sanction',NULL,'2024-09-12 23:07:37.677697','2024-09-12 23:07:37.677697'),
	 (2,'tpsg-gmt-poc-confirm',6,6,'sanctionChecked','enactDebitAccount',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.debit',NULL,'2024-09-12 23:07:37.722162','2024-09-12 23:07:37.722162'),
	 (2,'tpsg-gmt-poc-confirm',7,7,'enactDebitAccountDone','enactCreditAccount',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.credit',NULL,'2024-09-12 23:07:37.769388','2024-09-12 23:07:37.769388'),
	 (2,'tpsg-gmt-poc-confirm',8,8,'enactCreditAccountDone','prepareGsgr','','hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.gsgr',NULL,'2024-09-12 23:11:31.35981','2024-09-12 23:11:31.35981'),
	 (2,'tpsg-gmt-poc-confirm',9,8,'enactCreditAccountDone','gdMapping',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.gd',NULL,'2024-09-12 23:11:31.409109','2024-09-12 23:11:31.409109'),
	 (2,'tpsg-gmt-poc-confirm',10,9,'prepareGsgrDone','globalSettle',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.gsgr',NULL,'2024-09-12 23:11:31.455981','2024-09-12 23:11:31.455981'),
	 (2,'tpsg-gmt-poc-confirm',11,9,'prepareGsgrDone','globalRecon',NULL,'hrn.dev.wpb.rb.tb.bus','hrn.dev.wpb.rb.tb.gsgr',NULL,'2024-09-12 23:11:31.500176','2024-09-12 23:11:31.500176'),
	 (1,'tpsg-gmt-poc-propose',7,7,'fxPolicyProposed',NULL,NULL,'hrn.dev.wpb.rb.tb.bus',NULL,NULL,'2024-09-12 23:07:37.435462','2024-09-12 23:07:37.435462'),
	 (2,'tpsg-gmt-poc-confirm',12,10,'globalReconDone',NULL,'gdMapped & globalSettled & globalReconDone => complete;
globalSettled & globalReconDone => gsgrComplete','hrn.dev.wpb.rb.tb.bus',NULL,NULL,'2024-09-12 23:11:31.542899','2024-09-12 23:11:31.542899'),
	 (2,'tpsg-gmt-poc-confirm',13,10,'globalSettled',NULL,'gdMapped & globalSettled & globalReconDone => complete;
globalSettled & globalReconDone => gsgrComplete','hrn.dev.wpb.rb.tb.bus',NULL,NULL,'2024-09-12 23:11:31.59011','2024-09-12 23:11:31.59011');
INSERT INTO public.statemachine_control (use_case_id,use_case_name,id_in_case,step,from_state,to_state,state_formula,subscribe_topic,publish_topic,api_call_config,creation_time,last_update_time) VALUES
	 (2,'tpsg-gmt-poc-confirm',14,9,'gdMapped',NULL,'gdMapped & globalSettled & globalReconDone => complete;
globalSettled & globalReconDone => gsgrComplete','hrn.dev.wpb.rb.tb.bus',NULL,NULL,'2024-09-12 23:11:31.63886','2024-09-12 23:11:31.63886');
package com.mon4cc.database.entity;

import lombok.Data;
/**
 * Map the kafkaSpoutEntity class to database KafkaSpout table.
 * @author xjyou_
 * @Date 2020年12月15日
 */
@Data
public class KafkaSpout {
	
	/*Kafka spout id*/
	private String id ;
	
	/*Kafka spout name*/
	private String spoutComponentName ;
	
	/*Kafka spout parallelism*/
	private Integer spoutParallelism ;
	
	/*The kafka spout belongs to which topology*/
	private String topologyid ;
	
	/*Kafka spout code from web*/
	private String kafkaSpoutCode ;
	
	/*stream start with kafka spout. e.g. S1-S2*/
	private String kafkaSpoutStream ;
	/*
	 * kafka configuration as followed
	 */
	private String boostrapServer ;
	private int maxPollRecord ;
	private boolean autoCommit ;
	private String groupId ;
	private String offsetReset ;
	private String topic ;
}

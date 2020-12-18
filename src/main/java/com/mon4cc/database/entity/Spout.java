package com.mon4cc.database.entity;


import lombok.Data;
/**
 * Map SpoutEntity class to Spout table.
 * @author xjyou_
 * @Date 2020年12月15日
 */
@Data
public class Spout {
	
	/*Spout id*/
	private String id ;
	
	/*Spout name*/
	private String spoutComponentName ;
	
	/*spout parallelism*/
	private Integer spoutParallelism ;
	
	/*stream start with  spout. e.g. S1_S2*/
	private String spoutStream ;

	/*The spout belongs to which topology*/
	private String topologyid ;
	
	/*The spout code from web*/
	private String spoutCode ;
}

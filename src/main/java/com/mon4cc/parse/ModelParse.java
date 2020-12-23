/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mon4cc.parse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import com.mon4cc.entity.Bolt;
import com.mon4cc.entity.Grouping;
import com.mon4cc.entity.KafkaSpout;
import com.mon4cc.entity.Spout;
import com.mon4cc.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;

import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.junit.Before;

import com.mon4cc.parse.entity.ModelParseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

//@Component
@Configuration
public class ModelParse {
	protected BpmnModelInstance modelInstance;
	private String tid ;
	private String topologyName ;
	private boolean isLocal ;
	private String modelXml ;
	Collection<IntermediateThrowEvent> kafkaSpouts = null ;
	private static final Logger logger = LogManager.getLogger(ModelParse.class) ;

	@Autowired
	private IBoltService iBoltService;
	@Autowired
	private ITopologyconfigurationService iTopologyconfigurationService;
	@Autowired
	private ISpoutService iSpoutService;
	@Autowired
	private IKafkaspoutService iKafkaspoutService;
	@Autowired
	private IGroupingService iGroupingService;

	public String receiveAndParseData(ModelParseDTO modelParseDTO) {
	  
	  tid = modelParseDTO.getTid() ;
	  topologyName = modelParseDTO.getTopologyName() ;
	  isLocal = Boolean.parseBoolean(modelParseDTO.getIsLocal()) ;
	  
	  modelXml = modelParseDTO.getModelXml() ;

	  iTopologyconfigurationService.insertXml(tid,modelXml);// actually update operation

	  writeStringToFile(modelXml) ;
	  loadProcess() ;
	  return null ;
	}
  /*
   * Get bolt configuration based on BPMN task,
   * and insert into database ;
   */
  public boolean parseBolt() {
	  boolean flag = false ;
	  Collection<Task> bolts = modelInstance.getModelElementsByType(Task.class) ;

	  for(Task bolt: bolts) {
		 String medDatas = bolt.getId() ;
		 String[]data = medDatas.split("_") ;
		 
		 /*boltId, boltParallelism boltComponentName are need save to databased*/
		 String boltId = data[0]+"_"+data[1] ;
		 Integer boltParallelism = Integer.parseInt(data[2]) ;
		 
		 String boltStream = data[3] ;
		 
		 String boltComponentName = bolt.getName() ;

		  Bolt bolt1 = new Bolt();
		  bolt1.setId(boltId);
		  bolt1.setBoltParallelism(boltParallelism);
		  bolt1.setBoltStream(boltStream);
		  bolt1.setBoltComponentName(boltComponentName);
		  bolt1.setTopologyId(tid);

		  if (iBoltService.select_batch(bolt1.getId())){
			  iBoltService.update_batch(bolt1);
		  }else {
			  iBoltService.insert_batch(bolt1);
		  }

		  logger.info("Component Name: {}, Bolt Parallelism: {}",boltComponentName,boltParallelism) ;
	  }
	  flag = true ;
	  return flag ;
  }
  /*
   * Get grouping and stream configuration based on BPMN sequenceFlow,
   * and insert into database ;
   */
  public boolean parseGrouping() {
	  boolean flag = false ;
	  Collection<SequenceFlow> flows = modelInstance.getModelElementsByType(SequenceFlow.class) ;
	  for(SequenceFlow flow : flows) {
		  String [] datas = flow.getName().split("_") ;
		  
		  /* groupingId, sourceComponent,targetComponent,grouping,stream are need save database*/
		  String groupingId = flow.getId() ;
		  //grouping beginning which spout or bolt.
		  String sourceComponent = flow.getSource().getName() ;
		  //grouping ending which bolt
		  String targetComponent = flow.getTarget().getName() ;
		  //grouping name, e.g. shuffle grouping
		  String grouping = datas[0] ;
		  
		  //stream type in the grouping, e.g. S1, S2 are stream.
		  String stream = datas[1] ;

		  Grouping grouping1 = new Grouping();
		  grouping1.setGroupingId(groupingId);
		  grouping1.setSourceComponent(sourceComponent);
		  grouping1.setTargetComponent(targetComponent);
		  grouping1.setGrouping(grouping);
		  grouping1.setStream(stream);
		  grouping1.setTopologyId(tid);
		  if (iGroupingService.select_batch(grouping1.getGroupingId())){
			  iGroupingService.update_batch(grouping1);
		  }else {
			  iGroupingService.insert_batch(grouping1);
		  }




		  logger.info("Grouping: {}, Stream: {},Source: {},Target: {}",grouping,stream,sourceComponent,targetComponent) ;
	  }
	  flag = true ;
	  
	  return flag ;
  }
  /*
   * Get spout configuration based on BPMN startEvent,
   * and insert into database
   */
  public boolean parseSpout() {
	  boolean flag = false ;
	  Collection<StartEvent>spouts = modelInstance.getModelElementsByType(StartEvent.class) ;
		 for(StartEvent spout : spouts) {
			
			 String medData	= spout.getId() ;
			 String [] datas = medData.split("_") ;
			 
			 /*spoutId, spoutComponentName,spoutParallelism are need save in database*/
			 String spoutId = datas[0]+"_"+datas[1] ;
			 // get spout parallelism
			 Integer spoutParallelism =Integer.parseInt(datas[2]) ;
			 
			 String spoutStream = datas[3] ;
			 
			 // get spout component name
			 String spoutComponentName = spout.getName() ;

			 Spout spout1 = new Spout();
			 spout1.setId(spoutId);
			 spout1.setSpoutParallelism(spoutParallelism);
			 spout1.setSpoutStream(spoutStream);
			 spout1.setSpoutComponentName(spoutComponentName);
			 spout1.setTopologyId(tid);
			 if (iSpoutService.select_batch(spout1.getId())){
				 iSpoutService.update_batch(spout1);
			 }else {
				 iSpoutService.insert_batch(spout1);
			 }


			 
			 logger.info("Parallelism: {}, ComponentName: {}",spoutParallelism,spoutComponentName) ;
			 /*
			 Collection<SequenceFlow> outGroupings = spout.getOutgoing() ;
			 for(SequenceFlow outGrouping: outGroupings) {
				String[]groupingAndStream = outGrouping.getName().split("_") ;
				String group = groupingAndStream[0] ;
				String stream = groupingAndStream[1] ;
			 }
			 */
		 }
	  flag = true ;
	  
	  return flag ;
			  
  }
  
  /*
   *  Get kafka Spout Configuration based on BPMN intermediateThrowEvent,
   *  and insert into database
   */
  
 public boolean parseKafkaSpout() {
	 boolean flag = false ;
	 kafkaSpouts = modelInstance.getModelElementsByType(IntermediateThrowEvent.class) ;
	 for(IntermediateThrowEvent spout : kafkaSpouts) {
		 String kafkaSpout = spout.getId() ;
		 String[] datas = kafkaSpout.split("_") ;
		 /*kafkaSpoutId, kafkaSpoutComponentName, parallelism
		  *kafkaBootstrapAddress, maxPollRecords,enableAutoCommit
		  *,groupId,autoOffsetReset are need saved in database*/
		 String kafkaSpoutId = datas[0]+"_"+datas[1] ;
		 // get kafka spout name
		 String kafkaSpoutComponentName = spout.getName() ;
		 
		 // get kafka spout parallelism
		 int parallelism = Integer.parseInt(datas[2]) ;
		 //get S1-S2-S3
		 String kafkaSpoutStream = datas[3] ;
		 //kafka spout configuration
		 //bootstrap.servers
		 String kafkaBootstrapAddress = datas[4].replace('-', ':') ;
		 //max.poll.records
		 int maxPollRecords = Integer.parseInt(datas[5]) ;
		 //enable.auto.commit
		 boolean enableAutoCommit = Boolean.parseBoolean(datas[6]) ;
		 //group.id
		 String groupId = datas[7] ;
		 //auto.offset.reset
		 String autoOffsetReset = datas[8] ;
		 
		 String topic = datas[9] ;

		 KafkaSpout kafkaSpout1 = new KafkaSpout();
		 kafkaSpout1.setId(kafkaSpoutId);
		 kafkaSpout1.setSpoutComponentName(kafkaSpoutComponentName);
		 kafkaSpout1.setSpoutParallelism(parallelism);
		 kafkaSpout1.setKafkaSpoutStream(kafkaSpoutStream);
		 kafkaSpout1.setBoostrapServer(kafkaBootstrapAddress);
		 kafkaSpout1.setMaxPollRecord(maxPollRecords);
		 kafkaSpout1.setAutoCommit(enableAutoCommit);
		 kafkaSpout1.setGroupId(groupId);
		 kafkaSpout1.setOffsetReset(autoOffsetReset);
		 kafkaSpout1.setTopic(topic);
		 kafkaSpout1.setTopologyId(tid);

		 if (iKafkaspoutService.select_batch(kafkaSpout1.getId())){
			 iKafkaspoutService.update_batch(kafkaSpout1);
		 }else {
			 iKafkaspoutService.insert_batch(kafkaSpout1);
		 }


//		 System.out.println("Component Name :"+componentName+", parallelism: "+parallelism+", bootstrap.servers: "+
//		 kafkaBootstrapAddress+"\n"+"max.poll.records: "+maxPollRecords+", enable.auto.commit: "+enableAutoCommit+
//		 ", group.id: "+groupId+", auto.offset.reset: "+autoOffsetReset) ;
		 logger.info("Component Name :{}, parallelism: {}, bootstrap.servers: {},max.poll.records: {}, enable.auto.commit: {}, "
		 		+ "group.id: {}, auto.offset.reset: {}, topic: {}",kafkaSpoutComponentName,parallelism,kafkaBootstrapAddress
		 		,maxPollRecords,enableAutoCommit,groupId,autoOffsetReset,topic) ;
		 //kafka spout outgoing grouping
		 /*
		 Collection<SequenceFlow> outGroupings = spout.getOutgoing() ;
		 for(SequenceFlow outGrouping: outGroupings) {
			String[]groupingAndStream = outGrouping.getName().split("_") ;
			String group = groupingAndStream[0] ;
			String stream = groupingAndStream[1] ;
		 }
		 */
	 }
	 flag = true ;
	 
	 return flag ;

 }
 /*
 public void parseKafkaSpout(KafkaSpout kfks) {
	 
	 kafkaSpouts = modelInstance.getModelElementsByType(IntermediateThrowEvent.class) ;
	 for(IntermediateThrowEvent spout : kafkaSpouts) {
		 String kafkaSpout = spout.getId() ;
		 kfks.componentName.add(spout.getName()) ;
		 String[] datas = kafkaSpout.split("_") ;
		 
		 kfks.parallelism.add(Integer.parseInt(datas[2])) ;
		 //kafka spout configuration
		 //bootstrap.servers
		 kfks.kafkaBootstrapAddress.add(datas[3].replace('-', ':')) ;
		 //max.poll.records
		 kfks.maxPollRecords.add(Integer.parseInt(datas[4])) ;
		 //enable.auto.commit
		 kfks.enableAutoCommit.add(Boolean.parseBoolean(datas[5])) ;
		 //group.id
		 kfks.groupId.add(datas[6]) ;
		 //auto.offset.reset
		 kfks.autoOffsetReset.add(datas[7]) ;

		 //kafka spout outgoing grouping
		 Collection<SequenceFlow> outGroupings = spout.getOutgoing() ;
		 for(SequenceFlow outGrouping: outGroupings) {
			 
		 }
	 }
	 	logger.info("Parse Kafka Spout successeflly");

 }
 */
  /**
   * Write model of string type into file 
   * @param modelXml
   */
  public void writeStringToFile(String modelXml) {
	  //put string type of bpmn.model into file .
	  //File bpmnModel=new File("D:/java/Spring_mvcWorkSpace/mon4cc/src/main/resources/"+topologyName+".bpmn");
	  FileOutputStream fos = null ;
	  File bpmnModel=new File(getResourcePath()+"/"+topologyName+".bpmn") ;
      if(!bpmnModel.exists()){  
          try {
        	  bpmnModel.createNewFile() ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace() ;
		}  
     }
      byte bytes[]=new byte[512] ;   
      bytes=modelXml.getBytes() ;  
      int b=bytes.length ;   
      
	try {
		fos = new FileOutputStream(bpmnModel) ;
		fos.write(bytes,0,b) ;
		
		logger.info("Model writes to file successfully");
	    
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		logger.info("FileNotFoundException: "+e) ;
	} catch (IOException e) {
		// TODO Auto-generated catch block
		logger.info("IO Exception: "+e) ;
	} finally {
		if(fos != null)
			try {
				fos.close() ;
				logger.info("FileOutputStream is closed ") ;
			} catch (IOException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace() ;
				logger.info("FileOutputStream is closed failure: "+e) ;
			}
	}

  }
  
  /*
   * Get resource path
   */
  public String getResourcePath() {
	  String path = ModelParse.class.getClassLoader().getResource("").getPath() ;
	  logger.info("Get Resource Path: "+path) ;
	  return path ;
  }

  @Before
  public void loadProcess() {
    // read a BPMN model from an input stream
	 /*
    modelInstance = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream(topologyName+".bpmn")) ;
    logger.info("Read a BPMN model from an input stream ") ;
    */
	  modelInstance = Bpmn.readModelFromStream(getClass().getClassLoader().getResourceAsStream("demo.bpmn"));
  }

  
  public String parseXml(MultipartFile xmlFile) {
	  InputStream in = null ;
			try {
				in = xmlFile.getInputStream() ;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
	  modelInstance = Bpmn.readModelFromStream(in);
	  parseBolt();
	  parseSpout();
	  parseGrouping();
	  return "success";
  
  }

}

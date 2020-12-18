package com.mon4cc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mon4cc.parse.ModelParse;
import com.mon4cc.parse.entity.ModelParseDTO;

@RestController
@RequestMapping(value = "/mon4cc")
public class ParseController {
	
	@Autowired
	ModelParse modelParse ;
	
	@RequestMapping(value = "/model/info")
	public String parseObject(@RequestBody ModelParseDTO modelParseDTO) {
		
		return modelParse.receiveAndParseData(modelParseDTO) ;
	}
	
	@RequestMapping(value = "/model/code")
	public String codeSaved(){
		
		return null ;
	}

}

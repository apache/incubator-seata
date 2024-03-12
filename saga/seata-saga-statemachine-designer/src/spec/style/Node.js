/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import { assign, forEach } from 'min-dash';
import BaseSpec from '../BaseSpec';
import NodeStyle from './NodeStyle';
// import THUMBNAIL from '../icons/bpmn-icon-service-task.svg';


export default class Node extends BaseSpec {
  style = new NodeStyle();

  importJson(json) {
	 if(json.style === undefined){
		 json.style = {};
		json.style.bounds={x:200,y:200,width:36,height:36}
	} 
    assign(this.style.bounds, json.style.bounds);
  }
  

 isElementPresent(visited, target) {
    for (let element of visited) {
        if (element[0] === target[0] && element[1] === target[1]) {
            return false;
        }
    }
    return true;
}

 addWaypoints(source1,target1,definitions,Type1,targetwidth,targetheight,sourcewidth,sourceheight){
	 let sourcex=definitions.States[source1].style.bounds.x;
	 let sourcey=definitions.States[source1].style.bounds.y;
	 let targetx=definitions.States[target1].style.bounds.x;
	 let targety=definitions.States[target1].style.bounds.y;
	 let waypoints1=[];
	 if(Type1 === "Transition"){
	 	waypoints1=[{x:sourcex+sourcewidth,y:sourcey+(sourceheight/2)},{x:targetx-20,y:targety+(targetheight/2)},{x:targetx,y:targety+(targetheight/2)}];
	 }
	 else if(Type1 === "Compensation"){
		waypoints1=[{x:sourcex+(sourcewidth/2),y:sourcey+sourceheight},{x:targetx+(targetwidth/2),y:targety-20},{x:targetx+(targetwidth/2),y:targety}];
	 }
	 else{
		 if(sourcex === targetx){	
			waypoints1=[{x:sourcex+(sourcewidth/2),y:sourcey+sourceheight},{x:targetx+(targetwidth/2),y:targety-20},{x:targetx+(targetwidth/2),y:targety}];
	 	 }
	 	 else if(sourcey === targety){
			 waypoints1=[{x:sourcex+sourcewidth,y:sourcey+(sourceheight/2)},{x:targetx-20,y:targety+(targetheight/2)},{x:targetx,y:targety+(targetheight/2)}];
		  }
	 }
	 
	 const elementJson={
		 style:{
				waypoints:waypoints1,
				source:source1,
				target:target1
				},
			Type:Type1
	 }
	 return elementJson;
	 }

importEdges(definitions, startState) {
    if (startState.Next) {
        this.addEdge(startState.Name, startState.Next, definitions, "Transition", startState);
    }

    if (startState.CompensateState) {
        this.addEdge(startState.Name, startState.CompensateState, definitions, "Compensation", startState);
    }

    if (startState.Choices) {
        for (const option of startState.Choices) {
            this.addEdge(startState.Name, option.Next, definitions, "ChoiceEntry", startState);
        }
    }

    this.importJson(startState);
}

addEdge(source, target, definitions, type, startState) {
    const sourceWidth = this.calculateWidth(definitions.States[source]);
    const sourceHeight = this.calculateHeight(definitions.States[source]);
    const targetWidth = this.calculateWidth(definitions.States[target]);
    const targetHeight = this.calculateHeight(definitions.States[target]);

    const elementJson = this.addWaypoints(source, target, definitions, type, targetWidth, targetHeight, sourceWidth, sourceHeight);
    startState.edge = Object.assign(startState.edge || {}, { [target]: elementJson });
}

calculateWidth(state) {
    return (state.Type === "ServiceTask" || state.Type === "ScriptTask" || state.Type === "SubStateMachine") ? 100 : 36;
}

calculateHeight(state) {
    return (state.Type === "ServiceTask" || state.Type === "ScriptTask" || state.Type === "SubStateMachine") ? 80 : 36;
}


importCatchesEdges(definitions,startState){
	const catchEdges=startState.Catch;
	let edgesArray=[];
	let height=0,width=0;
	for(const option of catchEdges){
		
		let sourcex=definitions.States[startState.Name].catch.style.bounds.x;
		let sourcey=definitions.States[startState.Name].catch.style.bounds.y;
		let targetx=definitions.States[option.Next].style.bounds.x;
		let targety=definitions.States[option.Next].style.bounds.y;
		let waypoints1=[];
		if((definitions.States[option.Next].Type === "ServiceTask")||
		(definitions.States[option.Next].Type === "ScriptTask") || 
		(definitions.States[option.Next].Type === "SubStateMachine")){
			waypoints1=[{x:sourcex+18,y:sourcey},{x:targetx+50,y:targety+100},{x:targetx+50,y:(targety+100)-20}];
		}
		else{
			waypoints1=[{x:sourcex+18,y:sourcey},{x:targetx+18,y:(targety+36)+20},{x:targetx+18,y:targety+36}];
		}
			startState.catch.edge=assign(startState.catch.edge || {}, { [option.Next]: { "style": {"waypoints": waypoints1, "source": startState.Name,"target": option.Next}, "Type": "ExceptionMatch" } } );
		}
      this.importJson(startState.catch);
		
	}
	
addCatch(definitions,node,catchList,adjList){
	node.catch={};	
	if(node.Catch)
		{	
			const{style:{bounds:{x,y}}} =node;
			let newx=x;
			let newy=y;
			node.catch.style={};
			node.catch.style.bounds={
				x:newx+50,
				y:newy-20,
				width:36,
				height:36
			}
		}
	this.importJson(node.catch);
		
	let prev=node.catch;
	let width1,height1,begin;
	catchList.get(node).forEach((semantic) =>{
		
		if((semantic.Type === "ServiceTask") || (semantic.Type === "ScriptTask") || (semantic.Type === "SubStateMachine") ){
			width1=100;
			height1=80;
		}else{
			width1=36;
			height1=36;
		}
		semantic.style={};
		semantic.style.bounds={
			x:prev.style.bounds.x-50,
			y:prev.style.bounds.y-100,
			width:width1,
			height:height1,
		}
		prev=semantic;
		
		this.importStates(definitions,semantic,null,adjList);
	});
	
}

 importStates(definitions,startState,begin,adjList) {
	let flag=0;
    let visited=[];
		const queue=[];
		if(begin !== null){
		if(startState.style === undefined)
			{
				startState.style={};
					if (startState.style.bounds === undefined) 
					{
						startState.style.bounds={
		                x : begin.style.bounds.x + 150, // Adjust x-coordinate
		                y : begin.style.bounds.y , // Adjust y-coordinate
		                width:100,
		                height:80
		                }
					}
				}
				this.importJson(startState);
			}
			
		
	    queue.push(startState);
		while(queue.length){
			let currentState=queue.shift();
			
			adjList.get(currentState).forEach(neighbor =>{
				if(neighbor.style === undefined)
				{
					neighbor.style={};
					if((neighbor.Type === "Fail") || (neighbor.Type === "Succeed") ){
						const target=[];
						target.push(currentState.style.bounds.x+150,currentState.style.bounds.y);
						if(this.isElementPresent(visited,target)){
							setbounds(neighbor,currentState.style.bounds.x+150,currentState.style.bounds.y,36,36);
						}
						else{
							setbounds(neighbor,currentState.style.bounds.x,currentState.style.bounds.y+150,36,36);
						}
					}
						
					if((neighbor.Type === "ServiceTask" && !neighbor.IsForCompensation) || (neighbor.Type === "ScriptTask" && !neighbor.IsForCompensation) || (neighbor.Type === "SubStateMachine" && !neighbor.IsForCompensation))
					{	
						const target=[];
						target.push(currentState.style.bounds.x+150,currentState.style.bounds.y);
						if(this.isElementPresent(visited,target)){  
							setbounds(neighbor,currentState.style.bounds.x+150,currentState.style.bounds.y,100,80);
						 }
						else{
							setbounds(neighbor,currentState.style.bounds.x,currentState.style.bounds.y+150,100,80);
							}
				        
				         const{Name}=neighbor;
					     queue.push(definitions.States[Name]);
			        }
			        
			      	else if((neighbor.Type === "ServiceTask" && neighbor.IsForCompensation)  || (neighbor.Type === "ScriptTask" && neighbor.IsForCompensation) || ((neighbor.Type === "SubStateMachine" && neighbor.IsForCompensation)))
		             {
					  const target=[];
				      target.push(currentState.style.bounds.x,currentState.style.bounds.y+150);
					  if(this.isElementPresent(visited,target)){ 
							 setbounds(neighbor,currentState.style.bounds.x,currentState.style.bounds.y+150,100,80);
					 	  }  
	           		 }
	           		 
					 else if(neighbor.Type === "CompensationTrigger")
	           	      {
						   setbounds(neighbor,currentState.style.bounds.x,currentState.style.bounds.y-150,36,36);
						 	const{Name}=neighbor;
					        queue.push(definitions.States[Name]);
			          }
					  
					  else if(neighbor.Type === "Choice"){
						setbounds(neighbor,currentState.style.bounds.x+150,currentState.style.bounds.y,36,36);
						const{Name}=neighbor;
					    queue.push(definitions.States[Name]);
					  }
            	}
           
            	
			});
		}
		
		function setbounds(neighbor,x,y,width1,height1){
			if (neighbor.style.bounds === undefined)
				 {
					neighbor.style.bounds={
					     x : x, // Adjust x-coordinate
					     y : y, // Adjust y-coordinate
					     width:width1,
					     height:height1
				}
			 visited.push([x,y]);
			 }
		}
		
	}

  exportJson() {
    return assign({}, { style: this.style });
  }
}

package com.ociweb.pronghorn.util.parse;


import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.json.JSONAccumRule;
import com.ociweb.json.JSONType;
import com.ociweb.pronghorn.pipe.util.hash.LongHashTable;

public class JSONFieldMapping {
	
	private static final Logger logger = LoggerFactory.getLogger(JSONFieldMapping.class);
	
	private final JSONFieldSchema schema;
	private byte[] name;
	public final JSONType type;
	
	public JSONAccumRule accumRule;	
	public final boolean isAligned;
	private int[] values; //immutable once established
	private int dimensions; //immutable once established
	public String[] path; //immutable once established
	
	//capture first, last, all
	//align all values
	//
	
	
	public JSONFieldMapping(
					             JSONFieldSchema schema,
					             JSONType type,
					             boolean isAligned) {
	
		//we need all the paths first in order 
		//to ensure that the multiplier works.
		this.schema = schema;
		this.type = type;
		this.isAligned = isAligned;
		this.accumRule = null;//wait to set in setPath
	}

	public JSONFieldMapping(
					             JSONFieldSchema schema,
					             JSONType type,
					             boolean isAligned,
					             JSONAccumRule accumRule) {
	
		//we need all the paths first in order 
		//to ensure that the multiplier works.
		this.schema = schema;
		this.type = type;
		this.isAligned = isAligned;
		this.accumRule = accumRule; 
	}
	
	public void setName(String name) {
		this.name = name.getBytes();
	}
	
	public void setPath(JSONFieldSchema schema, String... path) {
		this.values = new int[path.length];
		int dimCounter = 0;
		for(int x = 0; x<path.length; x++) {			
			if (isArray(path[x])) {
				//TODO: do we want to support 0, 1-2 etc notation? do it here..
				dimCounter++;
				values[x] = -(x+1); //negative values to mark array usages
			} else {
				values[x] = schema.lookupId(path[x]);
			}
			//logger.info("lookupId for {} returned {} ", path[x], values[x]);
		}
		this.dimensions = dimCounter;
		schema.recordMaxPathLength(path.length);
		
		if (null == accumRule) {
			//set default value based on if this field is dimentional or not
			accumRule = (dimCounter==0)? JSONAccumRule.Last : JSONAccumRule.Collect;
		}
		
	}
	
	public int dimensions() {
		return dimensions;
	}
	

	private boolean isArray(String value) {
		return (value.startsWith("[") && value.endsWith("]"));
	}

	public static int addHashToTable(int fieldIdx, int dimsIdx,
			LongHashTable lookupFieldTableLocal,
			LongHashTable lookupDimentions,
			int[][] dimUsages,
			JSONFieldMapping jsonFieldMapping) {
		
		long pathHash = 0;//fieldIdx;
		int dimDepth = 0;
		for(int i = 0; i < jsonFieldMapping.values.length; i++) {
			long prev = pathHash*jsonFieldMapping.schema.maxFieldUnits();
			
			assert(jsonFieldMapping.values[i]!=0);
			if (jsonFieldMapping.values[i]>=0) {
				pathHash = prev + jsonFieldMapping.values[i];
				
			} else {
				//when value is less than 0 it is an array so the id,
				//must be moved up above the rest and the value is negative.
				pathHash = prev + (jsonFieldMapping.schema.uniqueFieldsCount()
						          -jsonFieldMapping.values[i]);
								
				int dimIdx;//lookup which fields make use of this array
				if (!LongHashTable.hasItem(lookupDimentions, pathHash)) {
					dimIdx = dimsIdx++;
					LongHashTable.setItem(lookupDimentions, pathHash, dimIdx);
				} else {
					dimIdx = LongHashTable.getItem(lookupDimentions, pathHash);					
				}

				dimDepth++; //must increment before it is stored.
				
				//detect if this is the first addition and record the dim depth
				if (dimUsages[dimIdx][0]==0) {
					//first insert so record specific dim depth 
					addToList(dimIdx, dimDepth, dimUsages);	//adds count plus this field
					
					//logger.info("dim dimDepth {} {} len {}", dimIdx, dimDepth, dimUsages[dimIdx][0]);
				} else {
					assert(dimUsages[dimIdx][1] == dimDepth) : "Internal error, dim depth must match";
				}
				
				addToList(dimIdx, fieldIdx, dimUsages);
				//logger.info("dim fieldIdx {} {} {} ", dimIdx, fieldIdx, Arrays.toString(dimUsages[dimIdx]));
				
				//dimDepth++; moved above for testing remove this line
			}
		
		}
		
		if (LongHashTable.hasItem(lookupFieldTableLocal, pathHash)) {
			throw new UnsupportedOperationException("field "+fieldIdx+" confilicts with previous field, each must be unique.");
		}
		
//		if (jsonFieldMapping.groupId>=0) {
//			//record the pathHash and/or fieldIdx as part of the group
//		    //store the fieldIdx to be looked up from the groupId,
//			
//			//what is the row for this groupId??
//			//addToList(row, fieldIdx, target);
//		}
		
		LongHashTable.setItem(lookupFieldTableLocal, pathHash, fieldIdx);
		return dimsIdx;
	}

	//record all the fieldIDs which will are part of the same groupId.
	//record all the fieldIDs which make use of this same dimIdx
	private static void addToList(int rowIdx, int addValue, int[][] targetList) {
		int count = targetList[rowIdx][0];
		
		//not gc free but only done on startup 
		if (count+2 > targetList[rowIdx].length) {
			//must grow since we have no room for len plus old data plus 1
			
			int[] usages = new int[count+2];
			System.arraycopy(targetList[rowIdx], 0, usages, 0, count+1);		
			targetList[rowIdx] = usages;
			
		}
		
		targetList[rowIdx][0] = ++count;
		targetList[rowIdx][count] = addValue;
				
	}

	public boolean nameEquals(byte[] name) {
		return Arrays.equals(name, this.name);
	}
	
}
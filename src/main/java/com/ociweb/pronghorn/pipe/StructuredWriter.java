package com.ociweb.pronghorn.pipe;

import com.ociweb.pronghorn.struct.StructRegistry;
import com.ociweb.pronghorn.struct.StructType;

public class StructuredWriter {

	private final DataOutputBlobWriter<?> channelWriter;
	
	public StructuredWriter(DataOutputBlobWriter<?> channelWriter) {
		this.channelWriter = channelWriter;
	}
	
	//////////////////////////
	//writes using associated object
	//////////////////////////
	
	private int pos = 0;
	private int[] positions = new int[4];
	private Object[] associations = new Object[4];

	/**
	 * Writes null to specified field in pipe
	 * calls selectStruct(id) only after setting all the object fields
	 * @param assoc field association showing where to write
	 */
	public void writeInt(Object assoc) {
		assert(DataOutputBlobWriter.getStructType(channelWriter)<=0) :  "call selectStruct(id) only after setting all the object fields.";
		storeAssocAndPosition(assoc);
		channelWriter.writePackedNull();
	}

	/**
	 * Writes int to specified field in pipe
	 * calls selectStruct(id) only after setting all the object fields
	 * @param assoc field association showing where to write
	 * @param value int to be written
	 */
	public void writeInt(Object assoc, int value) {
		assert(DataOutputBlobWriter.getStructType(channelWriter)<=0) :  "call selectStruct(id) only after setting all the object fields.";
		storeAssocAndPosition(assoc);
		channelWriter.writePackedInt(value);
	}

	/**
	 * Writes null to specified field in pipe
	 * calls selectStruct(id) only after setting all the object fields
	 * @param assoc field association showing where to write
	 */
	public void writeShort(Object assoc) {
		assert(DataOutputBlobWriter.getStructType(channelWriter)<=0) :  "call selectStruct(id) only after setting all the object fields.";
		storeAssocAndPosition(assoc);
		channelWriter.writePackedNull();
	}

	/**
	 * Writes short to specified field in pipe
	 * calls selectStruct(id) only after setting all the object fields
	 * @param assoc field association showing where to write
	 * @param value short to be written
	 */
	public void writeShort(Object assoc, short value) {
		assert(DataOutputBlobWriter.getStructType(channelWriter)<=0) :  "call selectStruct(id) only after setting all the object fields.";
		storeAssocAndPosition(assoc);
		channelWriter.writePackedInt(value);
	}

	/**
	 * Writes byte to specified field in pipe
	 * calls selectStruct(id) only after setting all the object fields
	 * @param assoc field association showing where to write
	 * @param value byte to be written
	 */
	public void writeByte(Object assoc, byte value) {
		assert(DataOutputBlobWriter.getStructType(channelWriter)<=0) :  "call selectStruct(id) only after setting all the object fields.";
		storeAssocAndPosition(assoc);
		channelWriter.write(value);
	}

	/**
	 * Writes CharSequence to specified field in pipe
	 * calls selectStruct(id) only after setting all the object fields
	 * @param assoc field association showing where to write
	 * @param text CharSequence to be written
	 */
	public void writeText(Object assoc, CharSequence text) {
		assert(DataOutputBlobWriter.getStructType(channelWriter)<=0) :  "call selectStruct(id) only after setting all the object fields.";
		storeAssocAndPosition(assoc);
		channelWriter.writeUTF(text);
	}

	/**
	 * Defines the record after fields are defined
	 */

	public void selectStruct(Object assoc) {
		selectStruct(Pipe.structRegistry(channelWriter.backingPipe).structLookupByIdentity(assoc));
	}

	/**
	 * Defines the record after fields are defined
	 */

	public void selectStruct(int structId) {
		
		StructRegistry structRegistry = Pipe.structRegistry(channelWriter.backingPipe);
		assert(DataOutputBlobWriter.getStructType(channelWriter)<=0) :  "call selectStruct(id) only after setting all the object fields.";
		DataOutputBlobWriter.commitBackData(channelWriter, structId);		
		int p = pos;
		while (--p>=0) {
			
			DataOutputBlobWriter.setIntBackData(channelWriter,
								positions[p],
								StructRegistry.lookupIndexOffset(structRegistry,
										              associations[p], structId, 
										              associations[p].hashCode()) & StructRegistry.FIELD_MASK					
							);
		}
		pos = 0;//cleared for next time;
	}

	
	
	///////////////////////

	private void storeAssocAndPosition(Object assoc) {
		if (null==assoc) {
			throw new NullPointerException("associated object must not be null");
		}
		grow(pos);

		int positionToKeep = channelWriter.position();
		//keep object
		positions[pos]=positionToKeep;
		associations[pos]=assoc;
		pos++;
	}
	
	
	private void grow(int pos) {
		if (pos==positions.length) {
			positions = grow(positions);
			associations = grow(associations);
		}
	}
	
	
	////////////////////////
	//writes using fieldId
	////////////////////////


	private Object[] grow(Object[] source) {
		Object[] result = new Object[source.length*2];
		System.arraycopy(source, 0, result, 0, source.length);
		return result;
	}

	private int[] grow(int[] source) {
		int[] result = new int[source.length*2];
		System.arraycopy(source, 0, result, 0, source.length);
		return result;
	}

	/**
	 * Writes blob to specified field in pipe
	 * calls selectStruct(id) only after setting all the object fields
	 * @param assoc field association showing where to write
	 * @return channelWriter
	 */
	public ChannelWriter writeBlob(Object assoc) {
		assert(DataOutputBlobWriter.getStructType(channelWriter)<=0) :  "call selectStruct(id) only after setting all the object fields.";
		storeAssocAndPosition(assoc);
		return channelWriter;
	}

	/**
	 * Writes blob to specified field in pipe
	 * @param fieldId field association showing where to write
	 * @return channelWriter
	 */
	public ChannelWriter writeBlob(long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Blob);
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		return channelWriter;
		
	}

	/**
	 * Writes text to specified field in pipe
	 * @param fieldId field to write to
	 * @return channelWriter
	 */
	public Appendable writeText(long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Text);
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		return channelWriter;
		
	}

	/**
	 * Writes boolean to specified field in pipe
	 * @param value true or false
	 * @param fieldId field to write to
	 */
	public void writeBoolean(boolean value, long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Boolean);
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		channelWriter.writeBoolean(value);
		
		assert confirmDataDoesNotWriteOverIndex(fieldId) : "Data has written over index data";
	}

	/**
	 * Writes null boolean to specified field in pipe
	 * @param fieldId field to write to
	 */
	public void writeBooleanNull(long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Boolean);
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		channelWriter.writeBooleanNull();
		
		assert confirmDataDoesNotWriteOverIndex(fieldId) : "Data has written over index data";
	}

	/**
	 * Writes boolean to specified field in pipe
	 * calls selectStruct(id) only after setting all the object fields
	 * @param assoc field association showing where to write
	 * @param value true or false
	 */
	public void writeBoolean(Object assoc, boolean value) {
		assert(DataOutputBlobWriter.getStructType(channelWriter)<=0) :  "call selectStruct(id) only after setting all the object fields.";
		storeAssocAndPosition(assoc);
		channelWriter.writeBoolean(value);
	}

	/**
	 * Writes long null to specified field in pipe
	 * @param fieldId field to write to
	 */
	public void writeLongNull(long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Long);
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		channelWriter.writePackedNull();
		
		assert confirmDataDoesNotWriteOverIndex(fieldId) : "Data has written over index data";
	}

	/**
	 * Writes long to specified field in pipe
	 * @param value long to be written
	 * @param fieldId field to write to
	 */
	public void writeLong(long value, long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Long);
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		channelWriter.writePackedLong(value);
		
		assert confirmDataDoesNotWriteOverIndex(fieldId) : "Data has written over index data";
	}

	/**
	 * Writes long to specified field in pipe
	 * calls selectStruct(id) only after setting all the object fields
	 * @param assoc field association showing where to write
	 * @param value long to be written
	 */
	public void writeLong(Object assoc, long value) {
		assert(DataOutputBlobWriter.getStructType(channelWriter)<=0) :  "call selectStruct(id) only after setting all the object fields.";
		storeAssocAndPosition(assoc);
		channelWriter.writePackedLong(value);
	}

	/**
	 * Writes long null to specified field in pipe
	 * calls selectStruct(id) only after setting all the object fields
	 * @param assoc field association showing where to write
	 */
	public void writeLongNull(Object assoc) {
		assert(DataOutputBlobWriter.getStructType(channelWriter)<=0) :  "call selectStruct(id) only after setting all the object fields.";
		storeAssocAndPosition(assoc);
		channelWriter.writePackedNull();
	}

	/**
	 * Writes int null to specified field in pipe
	 * @param fieldId field to write to
	 */
	public void writeIntNull(long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Integer);		
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		channelWriter.writePackedNull();
		
		assert confirmDataDoesNotWriteOverIndex(fieldId) : "Data has written over index data";
	}

	/**
	 * Writes int to specified field in pipe
	 * @param value int to be written
	 * @param fieldId field to be written to
	 */
	public void writeInt(int value, long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Integer);		
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		channelWriter.writePackedInt(value);
		
		assert confirmDataDoesNotWriteOverIndex(fieldId) : "Data has written over index data";
	}

	//TODO: this is fine for asserts but we need to check this before it happens like the old pub sub struct did.
	
	private boolean confirmDataDoesNotWriteOverIndex(long fieldId) {
		return channelWriter.position()< (Pipe.blobIndexBasePosition(channelWriter.backingPipe)-(4*Pipe.structRegistry(channelWriter.backingPipe)
				.totalSizeOfIndexes((int)(fieldId>>StructRegistry.STRUCT_OFFSET))));
	}

	/**
	 * Writes null to specified field in pipe
	 * calls selectStruct(id) only after setting all the object fields
	 * @param assocObject field association showing where to write
	 */
	public void writeNull(Object assocObject) {
		if (null==assocObject) {
			throw new NullPointerException("associated object must not be null");
		}
		grow(pos);

		int positionToKeep = -1;
		//keep object
		positions[pos]=positionToKeep;
		associations[pos]=assocObject;
		pos++;
	}

	/**
	 * Writes null to specified field in pipe
	 * @param fieldId field to be written to
	 */
	public void writeNull(long fieldId) {
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				-1, 
				StructRegistry.extractFieldPosition(fieldId));
	}
	
	public void writeShortNull(long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Short);
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		channelWriter.writePackedNull();
		
		assert confirmDataDoesNotWriteOverIndex(fieldId) : "Data has written over index data";
	}

	/**
	 * Writes short to specified field in pipe
	 * @param value short to be written
	 * @param fieldId field to be written to
	 */
	public void writeShort(short value, long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Short);
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		channelWriter.writePackedShort(value);
		
		assert confirmDataDoesNotWriteOverIndex(fieldId) : "Data has written over index data";
	}	
	
	//no support for writing null since this is a literal byte

	/**
	 * Writes byte to specified field in pipe
	 * @param value to be written
	 * @param fieldId field to be written to
	 */
	public void writeByte(int value, long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Byte);
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		channelWriter.writeByte(value);
		
		assert confirmDataDoesNotWriteOverIndex(fieldId) : "Data has written over index data";
	}	
	
	//for null use NaN, for all fields not written null is read..

	/**
	 * Writes double to specified field in pipe
	 * @param value double to be written
	 * @param fieldId field to be written to
	 */
	public void writeDouble(double value, long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Double);
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		channelWriter.writeDouble(value);
		
		assert confirmDataDoesNotWriteOverIndex(fieldId) : "Data has written over index data";
	}

	/**
	 * Writes float to specified field in pipe
	 * @param value float to be written
	 * @param fieldId field to be written to
	 */
	public void writeFloat(float value, long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Float);
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		channelWriter.writeFloat(value);
		
		assert confirmDataDoesNotWriteOverIndex(fieldId) : "Data has written over index data";
	}

	/**
	 * Writes rational to specified field in pipe
	 * calls selectStruct(id) only after setting all the object fields
	 * @param assoc field association showing where to write
	 * @param numerator of rational to be written
	 * @param denominator of rational to be written
	 */
	public void writeRational(Object assoc, long numerator, long denominator) {
		assert(DataOutputBlobWriter.getStructType(channelWriter)<=0) :  "call selectStruct(id) only after setting all the object fields.";
		storeAssocAndPosition(assoc);
		channelWriter.writeRational(numerator, denominator);
	}

	/**
	 * Writes rational to specified field in pipe
	 * @param numerator of rational to be written
	 * @param denominator of rational to be written
	 * @param fieldId field to be written to
	 */
	public void writeRational(long numerator, long denominator, long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Rational);
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		channelWriter.writeRational(numerator, denominator);
		
		assert confirmDataDoesNotWriteOverIndex(fieldId) : "Data has written over index data";
	}

	/**
	 * Writes decimal to specified field in pipe
	 * calls selectStruct(id) only after setting all the object fields
	 * @param assoc field association showing where to write
	 * @param m long to be written
	 * @param e byte to be written
	 */
	public void writeDecimal(Object assoc, long m, byte e) {
		assert(DataOutputBlobWriter.getStructType(channelWriter)<=0) :  "call selectStruct(id) only after setting all the object fields.";
		storeAssocAndPosition(assoc);
		channelWriter.writeDecimal(m, e);
	}

	/**
	 * Writes decimal to specified field in pipe
	 * @param m long to be written
	 * @param e byte to be written
	 * @param fieldId field to be written to
	 */
	public void writeDecimal(long m, byte e, long fieldId) {
		
		assert(Pipe.structRegistry(channelWriter.backingPipe).fieldType(fieldId) == StructType.Decimal);
		DataOutputBlobWriter.commitBackData(channelWriter, StructRegistry.extractStructId(fieldId));
		DataOutputBlobWriter.setIntBackData(
				channelWriter, 
				channelWriter.position(), 
				StructRegistry.extractFieldPosition(fieldId));
		
		channelWriter.writeDecimal(m, e);
		
		assert confirmDataDoesNotWriteOverIndex(fieldId) : "Data has written over index data";
	}

	public void fullIndexWriteFrom(int indexSizeInBytes, DataInputBlobReader<RawDataSchema> reader) {
		DataOutputBlobWriter.writeToEndFrom(channelWriter,indexSizeInBytes,reader);
	}
	
}

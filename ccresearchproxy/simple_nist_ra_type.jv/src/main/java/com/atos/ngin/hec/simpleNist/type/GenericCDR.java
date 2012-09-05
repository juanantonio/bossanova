package com.atos.ngin.hec.simpleNist.type;


public class GenericCDR implements Writable
{

	public static final char O_BRACKET = '<';
	public static final char C_BRACKET = '>';
	public static final char QUOTE = '"';
	public static final char SLASH = '/';
	public static final char EOL = '\n';
	public static final String HEADING = "<?xml version= \"1.0\" encoding=\"UTF-8\" ?>\n";
	public static final String O_RECORDS = "<records>";
	public static final String O_RECORDS_EOL = "<records>\n";
	public static final String C_RECORDS = "</records>";	
	public static final String C_RECORDS_EOL = "</records>\n";
	
	public static final String O_RECORD = "<record source=\"AS/SCP\" type=\"call\">";
	public static final String O_RECORD_EOL = "<record source=\"AS/SCP\" type=\"call\">\n";
	public static final String C_RECORD = "</record>";
	public static final String C_RECORD_EOL = "</record>\n";
	public static final String O_GROUP_GENERAL = "<group name=\"General\">";
	public static final String O_GROUP_GENERAL_EOL = "<group name=\"General\">\n";
	public static final String C_GROUP = "</group>";
	public static final String C_GROUP_EOL = "</group>\n";
	
	public static final String FIELD_INIT = "<field name=\"";
	public static final String FIELD_MIDDLE = "\" value=\"";
	public static final String FIELD_END = "/>";
	public static final String FIELD_END_EOL = "/>\n";
	
	public static final String SerialNum_Label = "SerialNumber";
	public long serialNumber;
	
	public static final String RecordType_Label = "RecordTpe";
	public String recordType;

	public static final String NodeID_Label = "NodeID";
	public String nodeID;

	public static String SequenceNumber_Label = "SequenceNumber";
	public String sequenceNumber;

	public static String StartDate_Label = "StartDate";
	public String startDate;

	public static String EndDate_Label = "EndDate";
	public String endDate;

	public static String CallDuration_Label = "CallDuration";
	public String callDuration;

	public static String CauseForRecordClosing_Label = "CauseForRecordClosing";
	public String causeForRecordClosing;

	public static String RecordTypeHEC_Label = "RecordTypeHEC";
	public String recordTypeHEC;		

	public String toString()
	{
		StringBuilder sb = new StringBuilder(500);
		sb.append(HEADING)
		.append(O_RECORDS_EOL);
		appendRecord(sb);
		sb.append(C_RECORDS_EOL);
		return sb.toString();
	}
	protected void appendRecord(StringBuilder sb)
	{
		sb.append(O_RECORD_EOL);
		appendGeneralGroup(sb);
		sb.append(C_RECORD_EOL);

	}

	protected void appendGeneralGroup(StringBuilder sb)
	{
		sb.append(O_GROUP_GENERAL_EOL);
		appendGeneralFields(sb);
		sb.append(C_GROUP_EOL);
		
	}
	protected void appendGeneralFields(StringBuilder sb)
	{
		sb.append(FIELD_INIT).append(RecordType_Label).append(FIELD_MIDDLE).append(recordType).append(FIELD_END_EOL)
		.append(FIELD_INIT).append(SerialNum_Label).append(FIELD_MIDDLE).append(serialNumber).append(FIELD_END_EOL)
		.append(FIELD_INIT).append(NodeID_Label).append(FIELD_MIDDLE).append(nodeID).append(FIELD_END_EOL)
		.append(FIELD_INIT).append(SequenceNumber_Label).append(FIELD_MIDDLE).append(sequenceNumber).append(FIELD_END_EOL)
		.append(FIELD_INIT).append(StartDate_Label).append(FIELD_MIDDLE).append(startDate).append(FIELD_END_EOL)
		.append(FIELD_INIT).append(EndDate_Label).append(FIELD_MIDDLE).append(endDate).append(FIELD_END_EOL)
		.append(FIELD_INIT).append(CallDuration_Label).append(FIELD_MIDDLE).append(callDuration).append(FIELD_END_EOL)
		.append(FIELD_INIT).append(CauseForRecordClosing_Label).append(FIELD_MIDDLE).append(causeForRecordClosing).append(FIELD_END_EOL)
		.append(FIELD_INIT).append(RecordTypeHEC_Label).append(FIELD_MIDDLE).append(recordTypeHEC).append(FIELD_END_EOL);
	}
	
}

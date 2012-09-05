package com.atos.ngin.hec.units.sbb.common;
							
public class Constants
{
  /**
   * General purpose
   */
  final public static String HEC_STATS_GROUP = "HEC";
  
  //CS COUNTERS
  public static final String ct_IMS_Call_Attempts = "IMS_Call_Attempts";
  public static final String ct_IMS_hec_mt = "IMS_hec_mt";
  public static final String ct_IMS_hec_mo = "IMS_hec_mo";
  public static final String ct_IMS_hec_ucm_outgoing = "IMS_hec_ucm_outgoing";
  public static final String ct_IMS_hec_ucm_incoming = "IMS_hec_ucm_incoming";
  public static final String ct_IMS_Unsupported_Trigger = "IMS_Unsupported_Trigger";
  public static final String ct_IMS_Trigger_Validated = "IMS_Trigger_Validated";
  public static final String ct_IMS_Diversion = "IMS_Diversion";
  public static final String ct_IMS_Released = "IMS_Released";
  public static final String ct_IMS_MO_NO_RR = "IMS_MO_NO_RR";
  public static final String ct_IMS_MO_RR = "IMS_MO_RR";
  public static final String ct_IMS_MT_NO_RR = "IMS_MT_NO_RR";
  public static final String ct_IMS_MT_RR = "IMS_MT_RR";
  public static final String ct_IMS_UCM_IN_PSTN = "IMS_UCM_IN_PSTN";
  public static final String ct_IMS_UCM_OUT_ONNET = "IMS_UCM_OUT_ONNET";
  public static final String ct_IMS_UCM_OUT_OFFNET = "IMS_UCM_OUT_OFFNET";
  public static final String ct_IMS_UCM_OUT_OFFNET_EMERGENCY = "IMS_UCM_OUT_OFFNET_EMERGENCY";
  public static final String ct_IMS_UCM_FW_FORK_ONNET = "IMS_UCM_FW_FORK_ONNET";
  public static final String ct_IMS_UCM_FW_FORK_OFFNET = "IMS_UCM_FW_FORK_OFFNET";
  public static final String ct_IMS_B_Special_Destination = "IMS_B_Special_Destination";
  public static final String ct_IMS_Force_On_PABX = "IMS_Force_On_PABX";
  public static final String ct_IMS_Breakout_Onnet = "IMS_Breakout_Onnet";
  public static final String ct_IMS_Breakout_Offnet = "IMS_Breakout_Offnet";
  public static final String ct_IMS_Correlation_Retrieved = "IMS_Correlation_Retrieved";
  public static final String ct_IMS_Correlation_Recorded = "IMS_Correlation_Recorded";
  public static final String ct_IMS_Correlation_Retrieval_Error = "IMS_Correlation_Retrieval_Error";
  public static final String ct_IMS_Correlation_Recording_Error = "IMS_Correlation_Recording_Error";

  
  //IMS COUNTERS
  public static final String ct_CS_IDPS = "CS_IDPS";
  public static final String ct_CS_IDPS_OK = "CS_IDPS_OK";
  public static final String ct_CS_IDPS_Invalid = "CS_IDPS_Invalid";
  public static final String ct_CS_IDPS_Invalid_Protocol = "CS_IDPS_Invalid_Protocol";
  public static final String ct_CS_Mobile_Originated = "CS_Mobile_Originated";
  public static final String ct_CS_Mobile_Terminated = "CS_Mobile_Terminated";
  public static final String ct_CS_Continue = "CS_Continue";
  public static final String ct_CS_Release = "CS_Release";
  public static final String ct_CS_Connect = "CS_Connect";
  public static final String ct_CS_MO_NO_RR = "CS_MO_NO_RR";
  public static final String ct_CS_MT_NO_RR = "CS_MT_NO_RR";
  public static final String ct_CS_MT_UCM_OUT = "CS_MT_UCM_OUT";
  public static final String ct_CS_MO_Rerouted_IMS = "CS_MO_Rerouted_IMS";
  public static final String ct_CS_MO_Rerouted_CS = "CS_MO_Rerouted_CS";
  public static final String ct_CS_MO_No_BCD = "CS_MO_No_BCD";
  public static final String ct_CS_MO_B_Public = "CS_MO_B_Public";
  public static final String ct_CS_MO_B_Special_Destination = "CS_MO_B_Special_Destination";
  public static final String ct_CS_MO_B_PNP_candidate = "CS_MO_B_PNP_candidate";
  public static final String ct_CS_MO_B_Nondecimal = "CS_MO_B_Nondecimal";
  public static final String ct_CS_MO_B_NOAI_International = "CS_MO_B_NOAI_International";
  public static final String ct_CS_MO_B_NOAI_National = "CS_MO_B_NOAI_National";
  public static final String ct_CS_MO_B_NOAI_Unknown = "CS_MO_B_NOAI_Unknown";  
  public static final String ct_CS_MO_B_Unsupported_NOAI = "CS_MO_B_Unsupported_NOAI";  
  public static final String ct_CS_MO_Roaming = "CS_MO_Roaming";
  public static final String ct_CS_MT_Rerouted_IMS = "CS_MT_Rerouted_IMS";
  public static final String ct_CS_MT_Rerouted_CS = "CS_MT_Rerouted_CS";
  public static final String ct_CS_Correlation_Retrieved = "CS_Correlation_Retrieved";
  public static final String ct_CS_Correlation_Recorded = "CS_Correlation_Recorded";
  public static final String ct_CS_Correlation_Retrieval_Error = "CS_Correlation_Retrieval_Error";
  public static final String ct_CS_Correlation_Recording_Error = "CS_Correlation_Recording_Error";  
  
  //SLEE Counters for threshold alarms
  public static enum HECUsageCounter
  {
	  INCOMING_MESSAGE,
	  INVALID_INCOMING_MESSAGE,	  
	  UNKNOWN_CALL_SCENARIO,
	  CORRELATION_ERROR
	  
  }  
  
  // OLD 
  /**
   * General purpose
   */
  final public static int    STR_BUILDERS_INI_SIZE = 160;
  final public static String HEC_STR               = "HEC";
  final public static String M2M_STR               = "M2M";

  /**
   * SLEE facilities
   */
  final public static String JNDI_MY_ENV       = "java:comp/env";
  final public static String JNDI_SLEE_FAC     = "slee/facilities/";
  final public static String JNDI_NULL_FAC     = "slee/nullactivity/";
  final public static String JNDI_TRACES_STR   = JNDI_SLEE_FAC + "trace";
  final public static String JNDI_TIMERS_STR   = JNDI_SLEE_FAC + "timer";
  final public static String JNDI_ALARMS_STR   = JNDI_SLEE_FAC + "alarm";
  final public static String JNDI_ACI_STR      = JNDI_SLEE_FAC +
                                                 "activitycontextnaming";
  final public static String JNDI_ACI_FAC_STR  = JNDI_NULL_FAC +
                                                 "activitycontextinterface" +
                                                 "factory";
  final public static String JNDI_NA_FAC_STR   = JNDI_NULL_FAC + "factory";
  final public static String JNDI_PROFILES_STR = JNDI_SLEE_FAC + "profile";
  
  final public static String STATSRA_TYPE      = "com/atos/ra/stat/api/";
  final public static String STATSRA_PROVIDER  = STATSRA_TYPE + "StatisticsProvider";
  final public static String STATSRA_FACTORY   = STATSRA_TYPE + "StatisticsACIFactory";
  
  public final static String CONFRA_TYPE       = "com/atos/ra/conf/api/";
  public final static String CONFRA_PROVIDER   = CONFRA_TYPE + "AtosConfigurationProvider";
  public final static String CONFRA_FACTORY    = CONFRA_TYPE + "AtosConfigurationACIFactory";

  /**
   * Regular expressions
   */
  public final static String SLASH_REGEXP      = "\\/";
  public final static String COMMA_REGEXP      = "\\,";
  public final static String UNDERSCORE_REGEXP = "\\_";
  public final static String PIPE_REGEXP       = "\\|";
  public final static String PERCENT_REGEXP    = "\\%";
  public final static String DOT_REGEXP        = "\\.";

  /**
   * Common characters and symbols
   */
  public final static String EMPTY_STRING        = "";
  public final static String UNDERSCORE          = "_";
  public final static char   CR                  = '\n';
  public final static String CR_STR              = "\n";
  public final static char   TAB                 = '\t';
  public final static String BLANK               = " ";
  public final static String PIPE_STR            = "|";
  public final static char   PIPE_CHAR           = '|';
  public final static char   PERCENT_CHAR        = '%';
  public final static String EQUALS              = "= ";
  public final static String COMMA               = ",";
  public final static String PLUS_STR            = "+";
  public final static String SLASH               = "/";
  public final static char   STAR                = '*';
  public final static String STAR_AS_A_STRING    = "*";
  public final static char   HASH                = '#';
  final public static byte   HYPHEN_AS_A_BYTE    = 45;
  final public static char   HYPHEN_AS_A_CHAR    = '-';
  final public static String HYPHEN_AS_A_STRING  = "-";
  final public static String OPENING_BRACKET     = "(";
  final public static String CLOSING_BRACKET     = ")";
  final public static char   AT                  = '@';
  
  /**
   * Strings for traces
   */
  public final static String ORIGINATOR_ADDRESS    = "Originator address: ";
  public final static String RECIPIENT_ADDRESS     = "Recipient Address: ";
  public final static String SESSION_ID            = "SessionId = ";
  public final static String ACI_DETACH            = "***** ACI DETACH *****";
  final public static String ERROR_STR             = "ERROR: ";
  final public static String NO_PREAMBLE           = " [NO PREAMBLE] ";
  final public static String GENERAL_TIMER_ACTIVE  = "General Timer Active:";
  final public static String PENDING_THIRD_PARTIES = "Pending Third Parties:";
  final public static String SENT_TO_THIRD_PARTIES = "Sent To Third Parties:";
  final public static String GENERAL_TIMER         = "General Timer:";
  final public static String RESPONSE_SENT         = "Response Sent:";
  final public static String THIRDPARTYACTIVITIES  = "ThirdPartyActivities:";
  final public static String COMMON_STATE          = "COMMON STATE:";
  final public static String NO                    = "NO";  
  final public static String HEC_NODE              = "HEC_NODE";
  final public static String M2M_NODE              = "M2M_NODE";
  public final static String NONE_STR              = "NONE";
  public final static String DEST                  = "DEST";
  public final static String ORIG                  = "ORIG";
  final public static String OK_STR                = "OK";
  final public static String KO_STR                = "KO";
  
}


/**
 * 
 * @author ¼¾Çï
 * 2019.02.27 Wednesday
 */
public class RepairParameters {

	// Constants	
	public final static String ALG1 = "Alg1";
	public final static String ALG2 = "Alg2";
	public final static String ALG3 = "Alg3";
	public final static String HST = "Hst";
	public final static String HSTSCORE = "HstScore";
	public final static String HSTWeight = "HstWeight";
	public final static String HSTSwoop = "HstSwoop";
	
	public final static String T_REAL = "real";		
	public final static String T_Pattern1 = "isolatedMUPS";
	public final static String T_Pattern2 = "cardiMin";
	public final static String T_Pattern3 = "cardiOverlap";
	public final static String T_Pattern4 = "cardiPattern4";
	public final static String T_SCAL = "ilpScalability";
	public final static String T_weights = "weights";
	public final static String T_random = "random";
	
	// Parameters to be configured
	public static int mupsNum = 30;
	public static int axiomNum = 5;
	public static int cardiMin = 6;		
	public static String repairStrategy = RepairParameters.ALG1; 
	public static String expType = T_REAL;	
	
	public static String system = "KEPLER";
	public static String o1Name = "edas";
	public static String o2Name = "ekaw";
	
	//public static String ontoName = RepairParameters.system+"-"+ RepairParameters.o1Name+"-"+RepairParameters.o2Name;
	public static String ontoName = "km1500-5000";//km1500-5000
	//public static String ontoName = "inco-mups"+mupsNum+"-ax"+axiomNum;
	//public static String ontoName = "inco-mups"+mupsNum+"-ax"+axiomNum+"-card"+cardiMin;
	public static String ontoPath = "data/ilp-data-"+expType+"/" + ontoName+".owl";
	public static String mupsPath = "results-mups-"+expType+"/"+ontoName+"/";		
	public static String diagPath = "results-diag-"+expType+"/"+ontoName+"/";
	public static String modelPath = "results-model-"+expType+"/"+ontoName+"/";	
}

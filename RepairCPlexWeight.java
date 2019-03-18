

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import edu.njupt.radon.exp.cplex2018.res.CollectMUPS;
import edu.njupt.radon.ilp.ILPTools;
import edu.njupt.radon.ilp.ILPWeights;

import edu.njupt.radon.parser.AlignmentParser;
import edu.njupt.radon.repair.RepairWithWeight;
import edu.njupt.radon.utils.CommonTools;
import edu.njupt.radon.utils.OWLTools;
import edu.njupt.radon.utils.io.FileTools;
import edu.njupt.radon.utils.io.PrintStreamObject;
import ilog.cplex.IloCplex;

public class RepairCPlexWeight {
	
	String repairStrategy = RepairParameters.repairStrategy;
	String resPath = RepairParameters.diagPath;
	
	public static void main(String[] args) throws Exception {		
		
		RepairCPlexWeight repair = new RepairCPlexWeight();
		if(RepairParameters.expType.equals(RepairParameters.T_random)) {
			repair.processRandomWeights();
		} else if(RepairParameters.expType.equals(RepairParameters.T_weights)) {
			repair.processMapping();
		}
		
		
	}
	
	public RepairCPlexWeight() {
		FileTools.checkPath(resPath+"models/");
	}
	
	public void processRandomWeights() throws Exception {
		// Obtain MUPS from the local file
		OWLOntology sourceOnto = OWLTools.openOntology(RepairParameters.ontoPath);		
		HashSet<HashSet<String>> ucMUPS = CollectMUPS.getMUPSStrFromText(RepairParameters.mupsPath);
		System.out.println("num of mupsstr: "+ucMUPS.size());
		//print(ucMUPS);
		//System.out.println("*********** end **********");
		
		HashSet<HashSet<OWLAxiom>> conflicts = CollectMUPS.transferStrToMUPS(sourceOnto, ucMUPS);
		System.out.println("num of conflicts: "+conflicts.size());		
		//CommonTools.printMultiSets(conflicts, null);
        
		
		// Get weights
		String resPath = RepairParameters.diagPath + "info/randomWeights.txt";
		HashMap<OWLAxiom, Double> weights = GenerateWeights.readWeights(sourceOnto, resPath);
		
		
		this.process(conflicts, weights);
	}
	
	public void print(HashSet<HashSet<String>> ucMUPS) {
		int i=1,j=1;
		for(HashSet<String> set : ucMUPS) {
			System.out.println((i++)+" mups");
			j = 1;
			for(String s : set) {
				System.out.println((j++)+"] "+s);
				if(s.contains("Uc 1")) {
					break;
				}
			}
			System.out.println();
		}
	}
	
	public void processMapping() throws Exception {		
		String sourceOntoPath = "data/oaei2018/"+RepairParameters.o1Name+".owl";
		String targetOntoPath = "data/oaei2018/"+RepairParameters.o2Name+".owl";
		String mappingPath = "data/oaei2018/results/"+RepairParameters.system+"-"+
		                     RepairParameters.o1Name+"-"+RepairParameters.o2Name+".rdf";	
		
		// Obtain MUPS from the local file
		OWLOntology sourceOnto = OWLTools.openOntology(RepairParameters.ontoPath);		
		HashSet<HashSet<String>> ucMUPS = CollectMUPS.getMUPSStrFromText(RepairParameters.mupsPath);
		System.out.println("num of mupsstr: "+ucMUPS.size());
		HashSet<HashSet<OWLAxiom>> conflicts = CollectMUPS.transferStrToMUPS(sourceOnto, ucMUPS);
		System.out.println("num of conflicts: "+conflicts.size());		
		
		//read the mapping and the weights between two source ontologies			
		OWLOntology o1 = OWLTools.openOntology("file:"+sourceOntoPath);	
		OWLOntology o2 = OWLTools.openOntology("file:"+targetOntoPath);	
		AlignmentParser align = new AlignmentParser(o1, o2);
		HashMap<OWLAxiom,Double> weights = align.readMappingsFromFile(mappingPath);	
		System.out.println("weights number : "+weights.size());
		ILPTools.assignWeights(sourceOnto, weights, 1);
		System.out.println("weights number after: "+weights.size());
		
		this.process(conflicts, weights);
	}
	
	public void process(HashSet<HashSet<OWLAxiom>> conflicts, 
			HashMap<OWLAxiom,Double> weights) throws Exception {
		HashSet<OWLAxiom> solution = new HashSet<OWLAxiom>();
		if(repairStrategy.equals(RepairParameters.ALG1)) {
			System.setOut((new PrintStreamObject(resPath+"Alg1-log.txt")).ps);	
			RepairCplex r = new RepairCplex();
			solution = r.repairByCPlex(resPath, conflicts);
			
		} else if(repairStrategy.equals(RepairParameters.ALG2)) {			
			System.setOut((new PrintStreamObject(resPath+"Alg2-log.txt")).ps);
			solution = repairByCPlexWeights(resPath, conflicts, weights);
			
		} else if(repairStrategy.equals(RepairParameters.ALG3)) {			
			System.setOut((new PrintStreamObject(resPath+"Alg3-log.txt")).ps);
			solution = repairByCPlexThreshold(resPath, conflicts, weights);	
			
		} else if(repairStrategy.equals(RepairParameters.HST)) {
			System.setOut((new PrintStreamObject(resPath+"Hst-log.txt")).ps);	
			RepairCplex r = new RepairCplex();
			solution = r.repairWithHst(conflicts);
			
		} else if(repairStrategy.equals(RepairParameters.HSTSCORE)) {
			System.setOut((new PrintStreamObject(resPath+"HstScore-log.txt")).ps);	
			RepairCplex r = new RepairCplex();
			solution = r.repairWithHstScore(conflicts);
			
		} else if(repairStrategy.equals(RepairParameters.HSTWeight)) {
			System.setOut((new PrintStreamObject(resPath+"HstWeight-log.txt")).ps);	
			solution = repairWithHstWeight(conflicts, weights);
			
		} else if(repairStrategy.equals(RepairParameters.HSTSwoop)) {
			System.setOut((new PrintStreamObject(resPath+"HstSwoop-log.txt")).ps);	
			SwoopHST t = new SwoopHST(conflicts, weights);
			solution = t.getSolution();
		} 
		System.out.println("*** Axioms removed: ");
		ILPTools.printAxioms(solution, weights);	
		System.out.println("************************************ \n");
	}
	
		
	public HashSet<OWLAxiom> repairWithHstWeight(HashSet<HashSet<OWLAxiom>> multiSets, 
			HashMap<OWLAxiom,Double> weights) {
		System.out.println("Number of conflicts: " + multiSets.size());		
		long st = System.currentTimeMillis();
		HashSet<HashSet<OWLAxiom>> diags = RepairWithWeight.getLowWeights(multiSets, weights);
		HashSet<OWLAxiom> minHS = RepairWithWeight.getOneDiagnoseByHST(diags);
		long time = System.currentTimeMillis() - st;		
		System.out.println("Time to compute diagnosis (ms): " + time);		
		return minHS;
	}
	
	
	// Compute diagnosis by CPlex with weights
	public HashSet<OWLAxiom> repairByCPlexWeights(String resPath, 
			HashSet<HashSet<OWLAxiom>> conflicts,
			HashMap<OWLAxiom,Double> weights) throws Exception {
		
		System.out.println("Number of conflicts: " + conflicts.size());
		HashSet<OWLAxiom> solution = new HashSet<OWLAxiom>();
		
		ArrayList<OWLAxiom> inputAxioms = ILPTools.getAxiomList(conflicts);	
		ILPWeights.saveInfo(resPath, conflicts, inputAxioms, weights);
		
		long st = System.currentTimeMillis();			
		ILPWeights.createModel(RepairParameters.diagPath+"models/", conflicts, inputAxioms, weights, 2);
		IloCplex cplex = new IloCplex();
		cplex.importModel(resPath+"models/ilpModel2.mps");
		cplex.setParam(IloCplex.Param.MIP.Pool.RelGap, 0.1);
		cplex.solve();
		solution = ILPTools.getCplexResult(cplex, inputAxioms);
		cplex.end();
		long time = System.currentTimeMillis() - st;
		
		System.out.println("Time to compute diagnosis (ms): " + time);
		return solution;
	}
	
	public HashSet<OWLAxiom> repairByCPlexThreshold(String resPath, 
			HashSet<HashSet<OWLAxiom>> multiSets,
			HashMap<OWLAxiom,Double> weights) throws Exception {
		
		System.out.println("Number of conflicts: " + multiSets.size());
		HashSet<OWLAxiom> solution = new HashSet<OWLAxiom>();
		
		ArrayList<OWLAxiom> inputAxioms = ILPTools.getAxiomList(multiSets);	
		double threshold = ILPTools.getThresholdWithMinMax(multiSets, weights);
		System.out.println("Threshold is :" +threshold+"\n");
		
		long st = System.currentTimeMillis();		
		//ILPWeightAlgorithm.saveInfo(resPath, conflicts, axiomsInMUPS, weights);
		ILPWeights.createModel(RepairParameters.diagPath+"models/", multiSets, inputAxioms, weights, threshold);
		IloCplex cplex = new IloCplex();
		cplex.importModel(resPath + "models/ilpModel3.mps");
		cplex.setParam(IloCplex.Param.MIP.Pool.RelGap, 0.1);
		cplex.solve();
		solution = ILPTools.getCplexResult(cplex, inputAxioms);
		cplex.end();
		long time = System.currentTimeMillis() - st;
		
		System.out.println("Time to compute diagnosis (ms): " + time);		
		return solution;
	}




}

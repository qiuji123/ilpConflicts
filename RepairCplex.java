

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import edu.njupt.radon.debug.incoherence.blackbox.ComputeMIPS;
import edu.njupt.radon.exp.cplex2018.res.CollectMUPS;
import edu.njupt.radon.ilp.ILPAlgorithm;
import edu.njupt.radon.ilp.ILPTools;

import edu.njupt.radon.repair.RepairWithScore;
import edu.njupt.radon.utils.CommonTools;
import edu.njupt.radon.utils.OWLTools;
import edu.njupt.radon.utils.io.FileTools;
import edu.njupt.radon.utils.io.PrintStreamObject;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.cplex.IloCplex;

public class RepairCplex {
	
	public static void main(String[] args) throws Exception {
		// "koala.owl","buggyPolicy.owl",
		// "University.owl","Terrorism.owl","miniTambis.owl","proton_50_studis.owl","CRS-SIGKDD.owl"
		// "Economy-SDA.owl",,"Transportation-SDA.owl","CONFTOOL-CMT.owl"

		// OWLTools.getTBox(ontology);
/*		String repairStrategy = RepairParameters.HST; 
		String ontoName = "inco-mups20-ax5-card2";
		String ontoPath = "data/generated2/" + ontoName+".owl";
		String mupsPath = "results-mups2/"+ontoName+"/";		
		String resPath = "results-diagnose2/"+ontoName+"/";*/
								
		OWLOntology sourceOnto = OWLTools.openOntology(RepairParameters.ontoPath);
		
		// Obtain conflicts
		//HashMap<OWLClass, HashSet<HashSet<OWLAxiom>>> ucMUPS = getMUPSFromDump(sourceOnto, mupsPath);
		//HashSet<HashSet<OWLAxiom>> conflicts = getConflicts(ucMUPS);
		
		HashSet<HashSet<String>> ucMUPS = CollectMUPS.getMUPSStrFromText(RepairParameters.mupsPath);
		System.out.println("num of mupsstr: "+ucMUPS.size());
		HashSet<HashSet<OWLAxiom>> conflicts = CollectMUPS.transferStrToMUPS(sourceOnto, ucMUPS);
		System.out.println("num of conflicts: "+conflicts.size());
				
		String repairStrategy = RepairParameters.repairStrategy;
		String resPath = RepairParameters.diagPath;
		
		FileTools.checkPath(RepairParameters.diagPath);
		FileTools.checkPath(RepairParameters.diagPath+"models/");
		
		RepairCplex r = new RepairCplex();
		HashSet<OWLAxiom> solution = new HashSet<OWLAxiom>();
		if(repairStrategy.equals(RepairParameters.ALG1)) {
			System.setOut((new PrintStreamObject(resPath+"alg1-log.txt")).ps);	
			solution = r.repairByCPlex(resPath, conflicts);
					
		} else if(repairStrategy.equals(RepairParameters.HST)) {
			FileTools.checkPath(resPath);
			System.setOut((new PrintStreamObject(resPath+"hst-log.txt")).ps);	
			solution = r.repairWithHst(conflicts);
			
		} else if(repairStrategy.equals(RepairParameters.HSTSCORE)) {
			System.setOut((new PrintStreamObject(resPath+"hstScore-log.txt")).ps);	
			solution = r.repairWithHstScore(conflicts);			
		} 

		System.out.println("***Axioms removed: ");
		CommonTools.printAxioms(solution);
		System.out.println("************************************ \n");
	}
	
	
	// Compute diagnosis by Radon
	public HashSet<OWLAxiom> repairWithHst(HashSet<HashSet<OWLAxiom>> multiSets) {		

		System.out.println("Number of conflicts: " + multiSets.size());
		
		long st = System.currentTimeMillis();		
		HashSet<OWLAxiom> minHS = RepairWithScore.getOneDiagnoseByHST(multiSets);
		long time = System.currentTimeMillis() - st;
				
		System.out.println("Time to compute diagnosis (ms): " + time);
		return minHS;
	}
	
	public HashSet<OWLAxiom> repairWithHstScore(HashSet<HashSet<OWLAxiom>> multiSets) {
		
		System.out.println("Number of conflicts: " + multiSets.size());
		
		long st = System.currentTimeMillis();
		HashSet<HashSet<OWLAxiom>> diags = RepairWithScore.getHighScores(multiSets);
		HashSet<OWLAxiom> minHS = RepairWithScore.getOneDiagnoseByHST(diags);
		long time = System.currentTimeMillis() - st;
		
		System.out.println("Time to compute diagnosis (ms): " + time);		
		return minHS;
	}

	// Compute diagnosis by CPlex
	public HashSet<OWLAxiom> repairByCPlex(String resPath, HashSet<HashSet<OWLAxiom>> multiSets)
			throws IloException, IOException {
		
		System.out.println("Number of conflicts: " + multiSets.size());
		// Extract all the axioms from set
		ArrayList<OWLAxiom> inputAxioms = ILPTools.getAxiomList(multiSets);	
		HashSet<OWLAxiom> solution = new HashSet<OWLAxiom>();
		
		long st = System.currentTimeMillis();
		// Translate OWL axioms to cplex representation as a model (.mps file)
		ILPAlgorithm.createModel(resPath+"models/", multiSets, inputAxioms);
		IloCplex cplex = new IloCplex();
		// Import the saved model
		cplex.importModel(resPath+"models/" + "ilpModel1.mps");
		// Set parameters for CPlex
		cplex.setParam(IloCplex.Param.MIP.Pool.RelGap, 0.1);
		// Begin to compute diagnosis
		cplex.solve();
		solution = ILPTools.getCplexResult(cplex, inputAxioms);
		long time = System.currentTimeMillis() - st;
		
		System.out.println("Time to compute diagnosis (ms): " + time);
		return solution;
	}


}

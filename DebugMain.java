

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;

import com.clarkparsia.owlapi.explanation.PelletExplanation;

import edu.njupt.radon.debug.incoherence.RadonDebug;
import edu.njupt.radon.debug.incoherence.relevance.RelevanceDebug;

import edu.njupt.radon.utils.OWLTools;
import edu.njupt.radon.utils.io.DumpFile;
import edu.njupt.radon.utils.io.FileTools;
import edu.njupt.radon.utils.io.PrintStreamObject;
import edu.njupt.radon.utils.reasoning.ReasoningTools;

public class DebugMain {
	
	public static void main(String[] args) throws Exception {
		
		FileTools.checkPath(RepairParameters.diagPath);
		OWLOntology sourceOnto = OWLTools.openOntology(RepairParameters.ontoPath);
		System.out.println("onto: "+RepairParameters.ontoPath);
		System.out.println("onto axiom number : "+sourceOnto.getLogicalAxiomCount());
		/*int i = 1;
		for(OWLAxiom a : sourceOnto.getLogicalAxioms()){
			System.out.println((i++)+"> "+a.toString());
		}
		*/
		//computeExplanationsByPellet(sourceOnto, resPath);
		computeExplanationsByRaDon(sourceOnto, RepairParameters.mupsPath );
	}
	
	public static void computeExplanationsByRaDon(OWLOntology sourceOnto, String resPath){
		
		FileTools.checkPath(resPath);	
		
		HashSet<OWLClass> allUnsat = ReasoningTools.getUnsatiConcepts(sourceOnto, OWLTools.manager);
		System.out.println("Number of Ucs: " + allUnsat.size());
		
		System.setOut((new PrintStreamObject(resPath+"res.txt")).ps);
		HashSet<OWLAxiom> axioms = OWLTools.getTBox(sourceOnto);
		System.out.println("axiom number: "+axioms.size());
		// HeuristicDebug RelevanceDebug
		RadonDebug debug = new RelevanceDebug(axioms);
		
		System.out.println("Number of Ucs: " + allUnsat.size()); 
		
		// Compute MUPS for all unsatisfiable concepts
		int i = 0;
		HashMap<OWLClass, HashSet<HashSet<String>>> ucMUPS = new HashMap<OWLClass, HashSet<HashSet<String>>>();
		for (OWLClass cl : allUnsat) {			
			i ++;
			System.out.println("Uc " + i + " : " + cl.toString());			
			if(i < 0)
				continue;
			HashSet<HashSet<OWLAxiom>> mups = debug.getMUPS(cl);
			HashSet<HashSet<String>> allMUPS = new HashSet<HashSet<String>>();
			for(HashSet<OWLAxiom> oneMUPS : mups) {				
        		HashSet<String> strs = new HashSet<String>();
        		for(OWLAxiom ax : oneMUPS) {
        			strs.add(ax.toString());
        		}
        		allMUPS.add(strs);
        	}
			ucMUPS.put(cl, allMUPS);
			//String localName = OWLTools.getLocalName(cl);			
			//DumpFile.dumpObject( resPath+i+"-mups-"+localName+".dump", ucMUPS);
			//break;
			/*if (i > 10) {
				break;
			}*/
		}
		
	}
	
	public static void computeExplanationsByPellet(OWLOntology sourceOnto, String resPath){
		System.setOut((new PrintStreamObject(resPath+"res.txt")).ps);
		
		PelletExplanation d = new PelletExplanation(sourceOnto, false);
		HashSet<OWLClass> allUnsat = ReasoningTools.getUnsatiConcepts(sourceOnto, OWLTools.manager);
		// Compute MUPS for all unsatisfiable concepts
		int i = 1;
		HashMap<OWLClass, HashSet<HashSet<String>>> ucMUPS = new HashMap<OWLClass, HashSet<HashSet<String>>>();
		for (OWLClass cl : allUnsat) {
			System.out.println("Uc " + (i++) + " : " + cl.toString());
			Set<Set<OWLAxiom>> mups = d.getUnsatisfiableExplanations(cl);
			
			HashSet<HashSet<String>> allMUPS = new HashSet<HashSet<String>>();
			int mupsNum = 1;
			for(Set<OWLAxiom> oneMUPS : mups) {	
				System.out.println("MUPS: "+(mupsNum++));
        		HashSet<String> strs = new HashSet<String>();
        		int axiomCounter = 1;
        		for(OWLAxiom ax : oneMUPS) {
        			System.out.println(" ["+(axiomCounter++)+"]  "+ax.toString());
        			strs.add(ax.toString());
        		}
        		allMUPS.add(strs);
        	}
			ucMUPS.put(cl, allMUPS);
			
		}
		DumpFile.dumpObject( resPath+"mups.dump", ucMUPS);
	}
		

	
	public static void printMUPS(HashMap<OWLClass, HashSet<HashSet<OWLAxiom>>> ucMUPS) {
		System.out.println("Input MUPS:");
		for(OWLClass oc : ucMUPS.keySet()) {
			System.out.println("UC: "+oc.toString());
			int i = 1;
			for(HashSet<OWLAxiom> set : ucMUPS.get(oc)) {
				System.out.println("    MUPS: "+ (i++));
				int j = 1;
				for(OWLAxiom ax: set) {
					System.out.println("    ["+ (j++)+"] "+ax.toString());
				}
			}
		}
	}

}

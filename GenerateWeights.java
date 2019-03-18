

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import edu.njupt.radon.utils.OWLTools;
import edu.njupt.radon.utils.io.FileTools;
import edu.njupt.radon.utils.io.PrintStreamObject;

public class GenerateWeights {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String resPath = RepairParameters.diagPath + "info/";
		FileTools.checkPath(resPath);
		
		OWLOntology sourceOnto = OWLTools.openOntology(RepairParameters.ontoPath);	
		System.setOut((new PrintStreamObject(resPath+"randomWeights.txt")).ps);
		int i = 1;
		for(OWLAxiom ax : sourceOnto.getLogicalAxioms()) {
			System.out.println((i++)+">"+ax.toString()+" : "+ generateRandomWeight());
		}

	}
	
	public static HashMap<OWLAxiom, Double> readWeights(OWLOntology sourceOnto, String resPath) throws Exception {
		
		// Obtain the correspondences between an axiom and its string from an ontology
		HashMap<String, OWLAxiom> strAxiomMap = new HashMap<String, OWLAxiom>();
		for(OWLAxiom ax : sourceOnto.getLogicalAxioms()) {
			strAxiomMap.put(ax.toString(), ax);
		}
		
		BufferedReader reader = new BufferedReader(new FileReader(resPath));
		String line, axiomStr, wStr;
		int index = 0;
		HashMap<OWLAxiom, Double> weights =  new HashMap<OWLAxiom, Double>();
		int i = 1;
		while ((line=reader.readLine())!=null) {
			index = line.indexOf(" : ");
			axiomStr = line.substring(line.indexOf(">")+1, index);
			wStr = line.substring(index+3);
			OWLAxiom ax = strAxiomMap.get(axiomStr);
			Double d = Double.valueOf(wStr);
			//System.out.println((i++)+"> " +ax.toString()+" : "+d);
			if(ax == null) {
				System.err.println(" axiom is null for axiom "+ax.toString());
			}
			weights.put(ax, d);
		}
		
		return weights;
	}
	
	
	public static Double generateRandomWeight() {
		double value = Math.random();
		if(value == 0 ) {
			value += 0.1;
		}		
		return value;
	} 

}

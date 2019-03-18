

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

public class SwoopHST {

	//public HashMap  axiomRanksMap; // ranking params
	//public HashSet<OWLAxiom> removedAxiomSet; 
	
	HashSet<HashSet<OWLAxiom>> mups;
	HashMap<OWLAxiom,Double> weights;
	
	public SwoopHST(HashSet<HashSet<OWLAxiom>> mups, HashMap<OWLAxiom,Double> weights) { 
		this.mups = mups;
		this.weights = weights;
	}

	public HashSet<OWLAxiom> getSolution() {
		System.out.println("Number of conflicts: " + mups.size());		
		long st = System.currentTimeMillis();		
		HashSet<OWLAxiom> solution = new HashSet<OWLAxiom>();
		List<ArrayList<OWLAxiom>> plans = solveUsingHST();
		double min = 1000;
		for(ArrayList<OWLAxiom> plan : plans) {
			double sum = this.pathRank(plan);
			if(sum < min) {
				min = sum;
				solution = new HashSet<OWLAxiom>(plan);
			}			
		}	
		long time = System.currentTimeMillis() - st;		
		System.out.println("Time to compute diagnosis (ms): " + time);	
		return solution;
	}
	
	private List<ArrayList<OWLAxiom>> solveUsingHST() {

		List<ArrayList<OWLAxiom>> plans = new ArrayList<ArrayList<OWLAxiom>>();

		// select any random mups as root of HST
		HashSet<OWLAxiom> root = (HashSet<OWLAxiom>) mups.iterator().next(); // any random MUPS

		// order axioms and add to stack
		List<OWLAxiom> stack = new ArrayList<OWLAxiom>();
		stack.addAll(orderAxioms(root));		
		//System.out.println(" stack (begin): ");
		//printAxioms(stack);
		
		//System.out.println(" root: ");
		//printAxioms(root);

		// initialize all other variables before proceeding to main loop
		List<OWLAxiom> path = new ArrayList<OWLAxiom>();
		double optimum = 1000; // some high value (upper bound)

		while (stack.size() != 0) {

			// always: last item on stack (i.e. tail of list) needs to be popped next
			int axiomIndex = stack.size() - 1;
			OWLAxiom axiom = (OWLAxiom) stack.get(axiomIndex);
			//System.out.println(" axiom popped: "+ axiom.toString()+" : "+weights.get(axiom));
			stack.remove(axiomIndex);
			//System.out.println(" stack (after pop): ");
			//printAxioms(stack);

			// when explored all options with this immediate edge from root
			// remove edge and proceed to next edge
			if (path.contains(axiom)) {
				path.remove(axiom);
				
				//System.out.println(" path (repeat): ");
				//System.out.println("    repeated axiom: "+axiom.toString()+" : "+weights.get(axiom));
				//printAxioms(path);
				continue;
			}

			path.add(axiom);
			//System.out.println(" path (new): ");
			//printAxioms(path);
			
			double pr = pathRank(path);

			// early path termination (check if it already exceeds optimum)
			if (pr >= optimum) {
				// remove from stack and backtrack
				path.remove(axiom);
				//System.out.println(" path (early): ");
				//printAxioms(path);
				continue;
			}
			
			
			// check if path is a Hitting Set (HS)
			HashSet<OWLAxiom> left = checkPathHS(path, mups);
			if (left.isEmpty()) {				
				optimum = pr;
				//System.out.println("optimum: "+optimum);
				//printAxioms(path);
				plans.add(new ArrayList<OWLAxiom>(path));
				path.remove(axiom);
			} else {
				// found new node to add to HST
				stack.add(axiom);
				stack.addAll(orderAxioms(left));
				//System.out.println(" stack (add new MUPS): ");
				//printAxioms(stack);
			}
		}

		return plans;
	}
	
	public void printAxioms(List<OWLAxiom> stack) {
		int i = 1;
		for(OWLAxiom a : stack) {
			System.out.println("   <"+(i++)+"> " +a.toString()+" : "+weights.get(a));
		}
		System.out.println();
	}
	
	public void printAxioms(HashSet<OWLAxiom> stack) {
		int i = 1;
		for(OWLAxiom a : stack) {
			System.out.println("   <"+(i++)+"> " +a.toString()+" : "+weights.get(a));
		}
		System.out.println();
	}

	public double pathRank(List<OWLAxiom> path) {
		double pr = 0;
		for (OWLAxiom axiom : path) {
			if (weights.containsKey(axiom))
				pr += weights.get(axiom);
		}
		return pr;
	}

	/*
	 * Check if a path of axioms is a Hitting Set (HS) for a collection of MUPS
	 * Return one MUPS if there is a MUPS that share no axioms with the given path.
	 */
	private HashSet<OWLAxiom> checkPathHS(List<OWLAxiom> path, HashSet<HashSet<OWLAxiom>> allMUPS) {
		for (HashSet<OWLAxiom> mups : allMUPS) {
			boolean hit = false;
			for (OWLAxiom axiom : path) {
				if (mups.contains(axiom)) {
					hit = true;
					break;
				}
			}
			if (!hit)
				return mups;
		}
		return new HashSet<OWLAxiom>(); // path is a HS
	}

	/*
	 * Order axioms in a MUPS such that lowest ranked axioms are at the tail of the
	 * list
	 */
	private List orderAxioms(HashSet<OWLAxiom> mups) {
		List<OWLAxiom> m = new ArrayList<OWLAxiom>(mups);
		OWLObject ordered[] = new OWLObject[m.size()];
		int ord = 0;
		for (Iterator iter = m.iterator(); iter.hasNext();)
			ordered[ord++] = (OWLObject) iter.next();

		for (int i = 0; i < m.size() - 1; i++) {
			OWLObject a = ordered[i]; // (OWLObject) m.get(i);
			for (int j = i + 1; j < m.size(); j++) {
				OWLObject b = ordered[j]; // (OWLObject) m.get(j);
				double rankA = -1;
				double rankB = -1;
				if (weights.containsKey(a))
					rankA = weights.get(a);
				if (weights.containsKey(b))
					rankB = weights.get(b);
				if (rankA < rankB) {
					// swap a, b in ordered
					OWLObject temp = ordered[j];
					ordered[j] = ordered[i];
					ordered[i] = temp;
				}
			}
		}
		List result = new ArrayList();
		for (int ctr = 0; ctr < ordered.length; ctr++)
			result.add(ordered[ctr]);
		return result;
	}
}
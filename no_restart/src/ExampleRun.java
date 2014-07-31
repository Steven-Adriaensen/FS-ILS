/*
 * author: Steven Adriaensen
 * date: 31/07/2014
 * contact: steven.adriaensen@vub.ac.be
 * affiliation: Vrije Universiteit Brussel
 */

import java.util.Date;

import SAT.SAT;
import AbstractClasses.HyperHeuristic;
import AbstractClasses.ProblemDomain;


public class ExampleRun {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long seed = new Date().getTime();
		
		//algorithm used (FS-ILS with default parameter settings)
		HyperHeuristic algo = new NoRestartFairShareILS(seed);
		
		//benchmark instance solved (4th instance in the Maximum Satisfiability problem domain)
		ProblemDomain problem = new SAT(seed);
		int instance = 3;
		problem.loadInstance(instance);
		
		//time we're allowed to optimize
		long t_allowed = 600000;
		algo.setTimeLimit(t_allowed);

		algo.loadProblemDomain(problem);
		
		//start optimizing
		System.out.println("Testing "+algo+" for "+t_allowed+" ms on "+problem.getClass().getSimpleName()+"["+instance+"]...");
		algo.run();

		//print out quality of best solution found
		System.out.println(algo.getBestSolutionValue());
	}

}

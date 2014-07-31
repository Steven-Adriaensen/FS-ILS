/*
 * author: Steven Adriaensen
 * date: 31/07/2014
 * contact: steven.adriaensen@vub.ac.be
 * affiliation: Vrije Universiteit Brussel
 * 
 * Version of FS-ILS without restart condition
 */

import java.util.Vector;

import AbstractClasses.HyperHeuristic;
import AbstractClasses.ProblemDomain;
import AbstractClasses.ProblemDomain.HeuristicType;

public class NoRestartFairShareILS extends HyperHeuristic {
	//the problem to be solved
	ProblemDomain problem;
	
	//parameters
	double T = 0.5;
	
	//memory locations
	int c_current = 0;
	int c_proposed = 1;
	
	//heuristic indices
	int[] llhs_pert;
	int[] llhs_ls;
	
	//search process information
	int[] news;
	int[] durations;
	double[] evaluations;
	
	//variables holding solution qualities
	double e_proposed;
	double e_current;
	
	//variables related to acceptance
	double mu_impr;
	int n_impr;
	
	//FairShareILS using default parameter settings
	public NoRestartFairShareILS(long seed) {
		super(seed);
	}
	
	//FairShareILS using temperature T
	public NoRestartFairShareILS(long seed, double T) {
		this(seed);
		this.T = T;
	}

	@Override
	protected void solve(ProblemDomain problem) {
		//initialize search
		setup(problem);
		//ILS
		while(!hasTimeExpired()){
			long before = getElapsedTime();
			int option = select_option();
			apply_option(option);
			durations[option] += getElapsedTime()-before + 1;
			if(!problem.compareSolutions(c_proposed, c_current) && accept()){
				news[option]++;
				e_current = e_proposed;
				problem.copySolution(c_proposed, c_current);
			}
			//update evaluation (SpeedNew)
			evaluations[option] = (1.0 + news[option])/durations[option];
		}
	}
	
	private void setup(ProblemDomain problem){
		//initialize all variables
		llhs_ls = problem.getHeuristicsOfType(HeuristicType.LOCAL_SEARCH);
		int[] mut_llh = problem.getHeuristicsOfType(HeuristicType.MUTATION);
		int[] rc_llh = problem.getHeuristicsOfType(HeuristicType.RUIN_RECREATE);
		llhs_pert = new int[mut_llh.length+rc_llh.length];
		for(int i = 0; i < mut_llh.length;i++){
			llhs_pert[i] = mut_llh[i];
		}
		for(int i = 0; i < rc_llh.length;i++){
			llhs_pert[i+mut_llh.length] = rc_llh[i];
		}
		this.problem = problem;

		news = new int[llhs_pert.length+1];
		durations = new int[llhs_pert.length+1];
		evaluations = new double[llhs_pert.length+1];
		for(int i = 0; i < evaluations.length;i++){
			evaluations[i] = Double.MAX_VALUE/(llhs_pert.length+1);
		}
		
		mu_impr = 0;
		n_impr = 0;
		
		problem.initialiseSolution(c_current);
		e_current = problem.getFunctionValue(c_current);
	}
	
	private int select_option(){
		//select an option proportional to its evaluation (RouletteWheel)
		//determine the norm
		double[] evaluations = this.evaluations;
		double norm = 0;
		for(int i = 0; i < evaluations.length;i++){
			norm += evaluations[i];
		}
		//select the option
		double p = rng.nextDouble()*norm;
		int selected = 0;
		double ac = evaluations[0];
		while(ac < p){
			selected++;
			ac += evaluations[selected];
		}
		return selected;
	}
	
	private void apply_option(int option){
		//apply the selected option (IteratedLocalSearch)
		//perturbation step
		if(option < llhs_pert.length){
			e_proposed = problem.applyHeuristic(llhs_pert[option],c_current,c_proposed);
		}else{
			problem.initialiseSolution(c_proposed);
			e_proposed = problem.getFunctionValue(c_proposed);
		}
		hasTimeExpired();
		//followed by local search
		localsearch();
	}
	
	private void localsearch(){
		Vector<Integer> active = new Vector<Integer>();
		for(int i = 0; i < llhs_ls.length;i++){
			active.add(llhs_ls[i]);
		}
		while(!active.isEmpty()){
			int index = rng.nextInt(active.size());
			double e_temp = problem.applyHeuristic(active.get(index),c_proposed,c_proposed);
			hasTimeExpired();
			if(e_temp < e_proposed){
				e_proposed = e_temp;
				active.clear();
				//restore
				for(int i = 0; i < llhs_ls.length;i++){
					active.add(llhs_ls[i]);
				}
			}else{
				active.remove(index);
			}
		}
	}
	
	private boolean accept(){
		//decides whether to accept c_proposed or not (AcceptProbabilisticWorse)
		if(e_proposed < e_current){
			n_impr++;
			mu_impr += (e_current-e_proposed-mu_impr)/n_impr;
		}
		return rng.nextDouble() < Math.exp((e_current-e_proposed)/(T*mu_impr));
	}
	
	@Override
	public String toString() {
		return "NoRestartFairShareILS(T:"+T+")";
	}

}

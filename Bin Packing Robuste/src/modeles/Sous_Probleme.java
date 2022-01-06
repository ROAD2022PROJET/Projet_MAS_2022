package modeles;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.cplex.IloCplex;

public class Sous_Probleme {

	private Données copy_data ;
	private IloCplex model;
	private IloObjective obj;
	private Map<Integer,IloNumVar> vars; // on associe a chaque size(item) une variable (ai) qui
	// indique si on le prend dans la configuation ou la colonne qu'on va générer (qu'on ajoute au MasterModel) 
	private IloNumVar mu ;
	private IloNumVar[] pi ;
	private Integer max_deviation ;

	public Sous_Probleme(Données data, Integer _max_deviation, boolean identical_deviation ) throws IloException
	{
		this.copy_data = new Données(data) ;
		this.model = new IloCplex();
		this.vars = new LinkedHashMap<>();
		this.max_deviation = _max_deviation ;

		initVars();
		initConstraint(identical_deviation);
		initObjective();
		model.setOut(null);
	}


	private void initVars() throws IloException
	{
		// init les x_i (ou comme je les ai appelé 'a_i'  pour pas qu'il y ait de confusion)
		for(int index_size=0;   index_size<this.copy_data.Nbr_Items;   index_size++) {
			IloNumVar var = model.boolVar("a["+index_size+"]") ; // on le prend ou pas 
			vars.put(index_size, var);
		}

		//init mu
		this.mu = model.numVar(0, Double.POSITIVE_INFINITY , "mu");

		// init les pi[i] (y'en a 'n')
		this.pi = new IloNumVar[this.copy_data.Nbr_Items] ;
		for(int i=0;  i<this.copy_data.Nbr_Items  ;i++) {
			this.pi[i] = model.numVar(0, Double.POSITIVE_INFINITY , "pi["+i+"]") ;
			//model.add(pi[i]) ;
		}
	}


	private void initObjective() throws IloException
	{
		obj = model.addMaximize();
	}


	private void initConstraint(boolean identical_deviation) throws IloException
	{
		// la 1ére contrainte (de capacité) ET la 2nd contrainte (des duales) 
		int j=0 ;
		//IloNumExpr expr1 = model.constant(0);
		IloLinearNumExpr expr1 = model.linearNumExpr();
		IloLinearNumExpr expr2 = model.linearNumExpr();

		if( ! identical_deviation ) { // tailles incertaines différentes  
			for (Entry<Integer,IloNumVar> e : vars.entrySet())
			{
				int index_size = e.getKey();
				IloNumVar var = e.getValue();
				expr1.addTerm(this.copy_data.Items_Sizes.get(index_size) , var);
				expr1.addTerm(1, pi[j]);


				// la 2nd contrainte 
				expr2.addTerm(1, this.pi[j]);
				expr2.addTerm(1, this.mu);
				expr2.addTerm(-this.copy_data.Deviations.get(j), var); // -d_i*a_i 
				j++ ; // pour itérer 
				model.addGe(expr2, 0);  
				System.out.println(expr2);
				expr2.clear();		
			}
			
			expr1.addTerm(this.max_deviation  , this.mu );
			System.out.println(expr1);
			model.addLe(expr1, this.copy_data.Size_Bin);  

			expr1.clear();
			expr2.clear();
		}

		else { // les parties incertaines sont identiques
			for (Entry<Integer,IloNumVar> e : vars.entrySet())
			{
				int index_size = e.getKey();
				IloNumVar var = e.getValue();
				expr1.addTerm(this.copy_data.Items_Sizes.get(index_size) , var);
				expr1.addTerm(1, pi[j]);

				// la 2nd contrainte 
				expr2.addTerm(1, this.pi[j]);
				expr2.addTerm(1, this.mu);
				expr2.addTerm(-this.copy_data.Deviations.get(0), var); // -d__{0}*a_i 
				j++ ; // pour itérer 
				model.addGe(expr2, 0);  
				expr2.clear();		
			}

			expr1.addTerm(this.max_deviation  , this.mu );

			model.addLe(expr1, this.copy_data.Size_Bin);  

			expr1.clear();
			expr2.clear();
		}
	}


	/*
	 * Mettre a jour l'Objectif a chauqe fois que le MasterModel tramsmet 
	 * ses variables Duales
	 * 
	 * Somme{alpha_i*a_i}
	 * duals : chauque index_size (ou Item_i) a sa variable duale (alpha_i) 
	 */
	public void setDuals(Map<Integer,Double> duals) throws IloException // 
	{
		// nouvelle expression pour l'objectif
		IloNumExpr expr = model.constant(0);
		for (Entry<Integer,Double> e : duals.entrySet())
		{
			int index_size = e.getKey();
			double dual = e.getValue();
			IloNumVar var = vars.get(index_size); // récuperer la var correspondante au size (de l'item)
			IloNumExpr term = model.prod(var, dual); // alpha_i * a_i
			expr = model.sum(expr, term);
		}
		// Remplacer l'objectif avec sa nouvelle expression
		obj.setExpr(expr);
	}


	// Effacer le model de la mémoire 
	public void cleanUp() throws IloException
	{
		model.clearModel();
		model.end();
	}

	// Résoudre le Modele 
	public void solve() throws IloException
	{	
		model.solve();
	}

	// retourne la valeur objective ( le meilleur cout réduit trouvé )	 
	public double getObjective() throws IloException
	{
		return model.getObjValue();
	}

	/*
	 * Retourner le Pattern (La configuration) associé a ce coût réduit trouvé  
	 */
	public Configuration getConfiguration() throws IloException
	{
		Map<Integer,Integer> resultMap = new LinkedHashMap<>();
		for (Entry<Integer,IloNumVar> e : vars.entrySet())
		{
			int index_size = e.getKey();
			IloNumVar var = e.getValue();
			int keep_ci = (int)Math.round(model.getValue(var));
			resultMap.put(index_size, keep_ci);
		}
		return new Configuration(resultMap);
	}
}


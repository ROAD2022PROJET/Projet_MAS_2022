package modeles;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class Maitre_2 {
	private double limite_arret = 0;

	private Donn�es copy_data;
	private IloCplex model;

	private IloObjective obj;
	private Integer max_deviation ;

	// 2 variables correspondant � 2 types de Bin
	private Map<Configuration,IloNumVar> vars_p;
	private Map<Configuration,IloNumVar> vars_q;

	private Map<Integer,IloRange> constraints;

	// 2 sous probl�mes 
	private Sous_Probleme_1 pricing_1 ;
	private Sous_Probleme_2 pricing_2 ;

	private double lowerbound = 0;
	private Solution solution;



	/*
	 * On cr�e le probleme Principal � partir des donn�es pass�es en argument 
	 */
	public Maitre_2(Donn�es data, Integer _max_deviation) throws IloException
	{

		this.copy_data = new Donn�es(data) ;
		this.model = new IloCplex();
		this.max_deviation = _max_deviation;

		this.vars_p = new LinkedHashMap<>();
		this.vars_q = new LinkedHashMap<>();

		this.constraints = new LinkedHashMap<>();

		this.pricing_1 = new Sous_Probleme_1(data,  _max_deviation);
		this.pricing_2 = new Sous_Probleme_2(data,  _max_deviation);

		System.out.println("**********  D�but Maitre 2  ********** \n");
		initConfigurationsAndVars();
		initConstraints();
		initObjective();
		model.setOut(null);
	}


	/*
	 * on Initialse en ouvrant un Bin pour chaque Item (article)
	 * Et donc l'Initialisation commence avec "n" Configurations (ou Patterns, n: nbre d'article)
	 */
	private void initConfigurationsAndVars() throws IloException
	{
		if(this.max_deviation == 0) { // Gamma == 0
			for(int i=0; i<this.copy_data.Nbr_Items; i++) {

				Map<Integer,Integer> BinInit = new LinkedHashMap<>();
				BinInit.put(i,1) ;

				for(int j=0; j<this.copy_data.Nbr_Items; j++) {
					if(j!=i) {
						BinInit.put(j, 0) ;
					}
				}
				Configuration Configuration = new Configuration(BinInit); // un pattern p
				IloNumVar var = model.numVar(0, Double.POSITIVE_INFINITY); // lambda 
				this.vars_q.put(Configuration, var) ;
			}
		}
		else { // Gamma >=1
			for(int i=0; i<this.copy_data.Nbr_Items; i++) {
				Map<Integer,Integer> BinInit = new LinkedHashMap<>();
				BinInit.put(i,1) ;

				for(int j=0; j<this.copy_data.Nbr_Items; j++) {
					if(j!=i) {
						BinInit.put(j, 0) ;
					}
				}
				Configuration Configuration = new Configuration(BinInit ); // un pattern p		
				IloNumVar var = model.numVar(0, Double.POSITIVE_INFINITY); // lambda 
				this.vars_p.put(Configuration, var) ;
			}
		}
		System.out.println("------> Configurations initialis�es");
	}


	/*
	 Initialise les contraintes
	 */
	private void initConstraints() throws IloException
	{
		for(int index_size=0; index_size<this.copy_data.Nbr_Items; index_size++) {
			IloNumExpr expr = model.constant(0);

			// pour les lambda_p
			if(! this.vars_p.isEmpty() ) {
				for (Entry<Configuration,IloNumVar> e : this.vars_p.entrySet()) // On parcourt tt les Pattern qui sont dans vars 
				{
					Configuration Configuration = e.getKey();
					if(Configuration.containsSize(index_size)) // si l'article est present dans le pattern 
					{										// ==> on mutiplie la variable qui correspnd au pattern (lambda_p)
						IloNumVar var = e.getValue();		// par 1 (a_ip == 1) {FIXE}
						IloNumExpr term = model.prod(var, 1 );  
						expr = model.sum(expr, term); // a_ip*lambda_p

					}
				}
			}
			// pour les lambda_q
			if(! this.vars_q.isEmpty() ) {
				for (Entry<Configuration,IloNumVar> e : this.vars_q.entrySet()) // On parcourt tt les Pattern qui sont dans vars 
				{
					Configuration Configuration = e.getKey();
					if(Configuration.containsSize(index_size)) // si l'article est present dans le pattern 
					{										// ==> on mutiplie la variable qui correspnd au pattern (lambda_p)
						IloNumVar var = e.getValue();		// par 1 (a_ip == 1) {FIXE}
						IloNumExpr term = model.prod(var, 1 );  
						expr = model.sum(expr, term); // a_ip*lambda_p

					}
				}
			}


			IloRange constraint = model.addEq(expr, 1); // Contrainte_i == 1
			constraints.put(index_size, constraint); // Car pour chaque "item" (Size) on a une Contrainte 
		}
		System.out.println("------> Contraintes initialis�es");
	}


	/*
	 *  Minimiser La somme des Variables de D�cision Somme{Lambda_p + Lambda_q}
	 */
	private void initObjective() throws IloException
	{
		//IloNumExpr expr = model.constant(0);
		IloLinearNumExpr expr = model.linearNumExpr();
		if( ! vars_p.isEmpty() ) {
			for (IloNumVar var : vars_p.values())    
			{
				expr.addTerm(1,var);
				//expr = model.sum(expr,var);
			}
		}
		if( ! vars_q.isEmpty() ) {
			for (IloNumVar var : vars_q.values())    
			{
				expr.addTerm(1,var);
				//expr = model.sum(expr,var);
			}
		}
		obj = model.addMinimize(expr);
		System.out.println("------> Fonction Objective initialis�e");
	}

	/*
	 *Ajouter des configuraions am�liorantes, des 2 sous probl�mes (comme exlpliqu� dans le Maitre 1) 
	 */
	private void addConfiguration(Configuration p, int nb_sub_pb) throws IloException
	{
		if ( ! vars_p.containsKey(p) && ! vars_q.containsKey(p) ) {

			// On cr�e la colonne avec un coeff == 1 dans l'objectif 
			IloColumn column = model.column(obj, 1);
			for (Entry<Integer,IloRange> e : constraints.entrySet())
			{
				int index_size = e.getKey();
				if (p.containsSize(index_size))  // On va remplir la colonne l� o� il faut (l� o� l'item est pr�sent)
				{
					// La contrainte associ�e a cet Article (Item)
					IloRange rng = e.getValue();
					// ajouter la contribution de "size" dans la contrainte 'rng' (qui est de 1)
					IloColumn coefficient = model.column(rng, 1);
					// On "M�lange" la Colonne 'coefficient' avec La colonne 'column'
					// qui a �t� d�ja initi�e (au d�but) avec son coeff dans l'objectif (=1)
					column = column.and(coefficient);
				}
			}

			// On cr�e une variable (lambda_p) pour cette colonne 
			IloNumVar var = model.numVar(column, 0, Double.POSITIVE_INFINITY);
			// On l'injecte avec les autres variables 
			if(nb_sub_pb == 1) {
				vars_p.put(p, var);
			}
			else { // nb_sub_pb == 2
				vars_q.put(p, var);	
			}
		}
	}


	/*
	 * R�cuperer les valeurs Duales des contraintes 
	 * Retourne une Map<l'Item (size), sa valeur duale>
	 */
	private Map<Integer,Double> getDuals() throws IloException
	{
		Map<Integer,Double> map = new LinkedHashMap<>();
		for (Entry<Integer,IloRange> e : constraints.entrySet())
		{
			// index of item (of size) 
			int index_size = e.getKey();

			// la Contrainte associ�e � cet item (� ce size) 
			IloRange constraint = e.getValue();
			// R�cuperer la valeur duale associ�e a la contrainte d'au dessus
			double dual = model.getDual(constraint);
			map.put(index_size, dual);
		}
		return map;
	}


	/*
	 * On g�nere une colonne, et on l'ajoute au model tant que 
	 * le cout reduit est positif (Pricing = Sac a dos (max profit) )
	 */
	private boolean generateColumn() throws IloException
	{
		Map<Integer, Double> duals = getDuals();
		boolean continue_pricing_1 = false;
		boolean continue_pricing_2 = false;

		// si Gamma >= 1  ( car si gamma == 0 alors les Bins qui seront g�n�r�s par sb1 seront vide )
		if(this.max_deviation != 0) {

			this.pricing_1.setDuals(duals);
			this.pricing_2.setDuals(duals);

			if(this.pricing_1.solve()) {
				Configuration Configuration_1 = pricing_1.getConfiguration();

				if (pricing_1.getObjective() > limite_arret && !vars_p.containsKey(Configuration_1))
				{
					addConfiguration(pricing_1.getConfiguration() , 1); // injecter la colonne trouv�e grace au Pricing_1 au model 	
					continue_pricing_1 = true ;
				}
				else {continue_pricing_1 = false ; }
			}

			if( this.pricing_2.solve()) {
				Configuration Configuration_2 = pricing_2.getConfiguration();

				if (pricing_2.getObjective() > limite_arret && !vars_q.containsKey(Configuration_2))
				{
					addConfiguration(pricing_2.getConfiguration(), 2); // injecter la colonne trouv�e grace au Pricing au model 
					continue_pricing_2 = true ;
				}
				else {continue_pricing_2 = false ; }
			}

			return ( continue_pricing_1 || continue_pricing_2) ;
		}

		else {
			this.pricing_2.setDuals(duals);
			this.pricing_2.solve();
			Configuration Configuration = pricing_2.getConfiguration();
			if (pricing_2.getObjective() > limite_arret && !vars_q.containsKey(Configuration))
			{
				addConfiguration(pricing_2.getConfiguration(), 2); // injecter la colonne trouv�e grace au Pricing au model 
				return true;
			}
			return false;
		}
	}

	/*
	 * Ralaxation optimale du Modele Maitre (LP-Relaxation)
	 */
	public void solveRelaxation() throws IloException
	{
		System.out.println("------> R�solution en cours ...");
		long max = 60000; // 60 seconde
		long arret = System.currentTimeMillis() + max;

		do
		{
			// R�soudre la Relaxation 
			model.solve();
			System.out.println(0.001*  ( arret - System.currentTimeMillis()) );
		} while (System.currentTimeMillis() < arret  && generateColumn() ); // Tant que de nouvelles colonnes avec un cout reduit et limite temps pas encore atteinte
		// positif sont g�n�r�es 

		System.out.println("------> R�solution termin�e. \n");
		lowerbound = model.getObjValue();	
		///// EN   PLUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUS
		// La solution bas�e sur la solution Enti�re
		Map<Configuration,Integer> result = new LinkedHashMap<>();
		if( ! vars_p.isEmpty() ) {
			for (Entry<Configuration,IloNumVar> e : vars_p.entrySet())
			{
				Configuration Configuration = e.getKey();
				IloNumVar var = e.getValue();
				int copies = (int)Math.round(model.getValue(var)); // int(du lambda_p trouv�)
				if (copies > 0)
				{
					result.put(Configuration, copies);
				}
			}
		}
		if( ! vars_q.isEmpty() ) {
			for (Entry<Configuration,IloNumVar> e : vars_q.entrySet())
			{
				Configuration Configuration = e.getKey();
				IloNumVar var = e.getValue();
				int copies = (int)Math.round(model.getValue(var)); // int(du lambda_p trouv�)
				if (copies > 0)
				{
					result.put(Configuration, copies);
				}
			}
		}
		//this.ExportModel(); // exporter le model dans un fichier (.lp) pour voir � quoi �a ressemble 
		this.solution = new Solution(result);
	}

	/*
	 * R�sout un PLNE (IP), on l'applique apr�s avoir �x�cuter la relaxation gr�ce 
	 * � la g�neration de colonne ( la m�thode d'au dessus ) 
	 */
	//	public void solveInteger() throws IloException
	//	{
	//		// Stocker la solution obtenue de la relaxation en tant que lower bound
	//		solveRelaxation();
	//		lowerbound = model.getObjValue();		
	//		// Convertir notre model en un model Entier et le r�soudre (en convertissant le type des Var en int)
	//		List<IloConversion> conversions = new ArrayList<>();
	//		for (IloNumVar var : vars.values())
	//		{
	//			IloConversion conv = model.conversion(var, IloNumVarType.Int);
	//			model.add(conv);
	//			conversions.add(conv);
	//		}
	//		model.solve();
	//
	//		// La solution bas�e sur la solution Enti�re
	//		Map<Configuration,Integer> result = new LinkedHashMap<>();
	//		for (Entry<Configuration,IloNumVar> e : vars.entrySet())
	//		{
	//			Configuration Configuration = e.getKey();
	//			IloNumVar var = e.getValue();
	//			int copies = (int)Math.round(model.getValue(var)); // int(du lambda_p trouv�)
	//			if (copies > 0)
	//			{
	//				result.put(Configuration, copies);
	//			}
	//		}
	//		// this.ExportModel(); // exporter le model dans un fichier (.lp) pour voir � quoi �a ressemble 
	//		//this.solution = new Solution(result);
	//
	//		// Annuler la conversion en Entiers
	//		for (IloConversion conv : conversions)
	//		{
	//			model.remove(conv);
	//		}
	//	}

	/*
	 * Effacer le mod�le Cplex car il n'est pas 
	 * ramass� par le Garbage Collector 
	 */
	public void cleanUp() throws IloException
	{
		model.clearModel();
		model.end();
		pricing_1.cleanUp();
		pricing_2.cleanUp();
	}

	// Retourner la solution trouv�e et stock�e dans 
	// l'attribut 'this.solution'
	public Solution getSolution()
	{
		return this.solution;
	}

	// Retourne la Borne Inf du probleme
	public double getLowerBound()
	{
		return lowerbound;
	}

	// si on veut exporter le Model pour voir � quoi �a ressemble 
	public void ExportModel() throws IloException {
		this.model.exportModel("Solution.lp");
	}





}

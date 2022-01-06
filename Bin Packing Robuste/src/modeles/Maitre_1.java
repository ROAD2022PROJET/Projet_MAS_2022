package modeles;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class Maitre_1 {
	private double limite_arret = 0;

	private Donn�es copy_data;
	private IloCplex model;

	private IloObjective obj;
	private Map<Configuration,IloNumVar> vars;
	private Map<Integer,IloRange> constraints;

	private Sous_Probleme pricing;

	private double lowerbound = 0;
	private Solution solution;



	/*
	 * On cr�e le probleme Principal � partir des donn�es pass�es en argument 
	 */
	public Maitre_1(Donn�es data, Integer _max_deviation, boolean identical_deviation) throws IloException
	{
		this.copy_data = new Donn�es(data) ;
		this.model = new IloCplex();
		this.vars = new LinkedHashMap<>();
		this.constraints = new LinkedHashMap<>();
		this.pricing = new Sous_Probleme(data,  _max_deviation, identical_deviation);

		System.out.println("**********  D�but Maitre 1  **********\n ");
		initConfigurationsAndVars();
		initConstraints();
		initObjective();
		model.setOut(null);
	}


	/*
	 * on Initialse en ouvrant un Bin pour chaque Item (article)
	 * Et donc l'Initialisation commence avec "n" Configurations (ou Patterns, n : nbre d'article)
	 */
	private void initConfigurationsAndVars() throws IloException
	{

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
			vars.put(Configuration, var); // lambda(p) 
		}
		System.out.println("------> Configurations initialis�es");
	}


	/*
	 On initialise les contraintes pour satisfaire les demandes de chauque Item  
	 */
	private void initConstraints() throws IloException
	{
		for(int index_size=0; index_size<this.copy_data.Nbr_Items; index_size++) {
			IloNumExpr expr = model.constant(0);
			for (Entry<Configuration,IloNumVar> e : vars.entrySet()) // On parcourt tt les Pattern qui sont dans vars 
			{
				Configuration Configuration = e.getKey();
				if(Configuration.containsSize(index_size)) // si l'article est present dans le pattern 
				{										// ==> on mutiplie la variable qui correspnd au pattern (lambda_p)
					IloNumVar var = e.getValue();		// par 1 (a_ip == 1) {FIXE}
					IloNumExpr term = model.prod(var, 1 );  
					expr = model.sum(expr, term); // a_ip*lambda_p

				}
			}
			IloRange constraint = model.addEq(expr, 1); // Contrainte_i == 1
			constraints.put(index_size, constraint); // Car pour chaque "item" (Size) on a une Contrainte 
		}
		System.out.println("------> Contraintes initialis�es");
	}


	/*
	 *  Minimiser La somme des Variables de D�cision {Lambda_p}
	 */
	private void initObjective() throws IloException
	{
		IloNumExpr expr = model.constant(0);
		for (IloNumVar var : vars.values())    
		{
			expr = model.sum(expr,var);
		}
		obj = model.addMinimize(expr);
		System.out.println("------> Fonction Objective initialis�e");

	}


	/*
	 *Cette m�thode est utilis�e pour ajouter une nouvelle configuration (colonne) au mod�le actuel.
	 * Transmise par le sous probl�me "Pricing" (la meilleure colonne avec le meilleur co�t r�duit)
	 * On cr�e cette nouvelle colonne, en cr�ant 1 variable de d�cision {lambd_p} qui corespond 
	 * a cette nouvelle configuration (avec un coeff == 1 dans l'objectif), et puis on l'ajoute aux contraintes  
	 * et enfin on l'ajoute � l'ensemble des variables 
	 */
	private void addConfiguration(Configuration p) throws IloException
	{
		if (vars.containsKey(p)) {
			// �a n'arrive jamais normalement, si tout est bon  
			throw new IllegalArgumentException("Cette colonne existe d�ja");
		}

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
		vars.put(p, var);
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
		pricing.setDuals(duals);
		pricing.solve();
		Configuration Configuration = pricing.getConfiguration();
		if (pricing.getObjective() > limite_arret && !vars.containsKey(Configuration))
		{
			addConfiguration(pricing.getConfiguration()); // injecter la colonne trouv�e grace au Pricing au model 
			return true;
		}
		return false;
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

		//------------------   EN   PLUUUUS-------------------------------------
		// La solution bas�e sur la solution Enti�re
		Map<Configuration,Integer> result = new LinkedHashMap<>();
		for (Entry<Configuration,IloNumVar> e : vars.entrySet())
		{
			Configuration Configuration = e.getKey();
			IloNumVar var = e.getValue();
			int copies = (int)Math.round(model.getValue(var)); // int(du lambda_p trouv�)
			if (copies > 0)
			{
				result.put(Configuration, copies);
			}
		}
		// this.ExportModel(); // exporter le model dans un fichier (.lp) pour voir � quoi �a ressemble 
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
		pricing.cleanUp();
	}


	/* Retourner la solution trouv�e et stock�e dans
	 * l'attribut 'this.solution' */
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

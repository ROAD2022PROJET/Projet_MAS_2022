package Application;
import modeles.*;

import java.io.File;
import java.io.FileNotFoundException;

import ilog.concert.IloException;

public class Main{

	public static void main(String[] args) throws FileNotFoundException, IloException {

		// Charger les données
		File f = new File("Instances\\r100_1000_1.txt"); 
		Données data = new Données(f);

		// l'incertitude 
		Integer Gamma = 2 ;

		// utiliser le modèle et les relaxation 'avec' ou 'sans' déviation
		boolean identical_deviation = true ;

		// heure de début	
		long time = System.currentTimeMillis();


		//-------------------------------------------------------------------------------------

		//Relax du Modèle de base (Berstimas et Sim, 2004)
		System.out.println("**********  Début Relaxation Modèle de Base  **********  ");
		BSim_2004_Relax.solve(data, Gamma, identical_deviation);
		time = System.currentTimeMillis() - time;
		System.out.println("Runtime: "+0.001*time+" s");
		System.out.println("\n-------------------------------------\n");	


		//-------------------------------------------------------------------------------------

		/*		
		// Modèle de base (Berstimas et Sim, 2004)
		System.out.println(" **********  Début Modèle de Base  **********  ");
		BSim_2004.solve(data, Gamma, identical_deviation);
		time = System.currentTimeMillis() - time;
		System.out.println("Runtime: "+0.001*time+" s");
		System.out.println("\n-------------------------------------\n");		


		//-------------------------------------------------------------------------------------


		//Relaxation linéaire de la reformulation de Dantzig-Wolfe, calculée par génération de colonnes
		Maitre_1 Mdm = new Maitre_1(data,Gamma, identical_deviation); // le 2e argument est Gamma, et le dernier c'est un booleen {parties incertaines identiques == true} 
		Mdm.solveRelaxation();
		double LB = Mdm.getLowerBound();  // La valeur de la Relaxation (LB) 
		Solution sol = Mdm.getSolution();

		System.out.println("LB: "+LB);
		System.out.println("");

		Mdm.cleanUp();

		time = System.currentTimeMillis() - time;
		System.out.println("Runtime: "+0.001*time+" s");
		System.out.println("\n-------------------------------------\n");


		//-------------------------------------------------------------------------------------


		//Relaxation pour notre cas particulier d_i == d, calculée par génération de colonnes 
		Maitre_2 Mdm2 = new Maitre_2(data,Gamma); // le second argument est Gamma
		Mdm2.solveRelaxation();
		double LB2 = Mdm2.getLowerBound();  // La valeur de la Relaxation (LB) 
		Solution sol2 = Mdm2.getSolution();

		System.out.println("LB2: "+LB2);
		System.out.println("");

		Mdm2.cleanUp();

		time = System.currentTimeMillis() - time;
		System.out.println("Runtime: "+0.001*time+" s");
		System.out.println("\n-------------------------------------\n");


		//-------------------------------------------------------------------------------------

		 */

	}
}

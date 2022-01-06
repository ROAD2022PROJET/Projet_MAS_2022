package modeles;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class Données {

	public int Size_Bin ; // la taille du bin 
	public int Nbr_Items ;  // le nombre d'artciles 
	public ArrayList<Integer>  Items_Sizes; // les tailles des articles
	public ArrayList<Integer> Deviations; // les deeviations des articles
	public int identical_deviation; // deviation
	public Map<Integer,Integer> Size_Deviation; // pour avoir les 2 (size-deviation) en un seul 



	public Données(File f) throws FileNotFoundException {
		try(Scanner scan = new Scanner(f)){
			this.Size_Bin = scan.nextInt(); // la taille du Bin 
			scan.nextLine();
			this.Nbr_Items = scan.nextInt(); // le nombre d'articles 

			this.Items_Sizes = new ArrayList<>();
			this.Deviations = new ArrayList<>();

			for(int i=0; i<this.Nbr_Items ; i++) {
				scan.nextLine();
				this.Items_Sizes.add( scan.nextInt() ) ;
				this.Deviations.add( scan.nextInt() ) ;
			}

			this.identical_deviation = this.Deviations.get(0) ;
			scan.close();
			
			this.Size_Deviation = this.Size_Deviation_map() ;
		}
	}

	// pour faire des copies et ne pas modifier l'original (eventuellement)
	public Données( Données dat ) { 
		this.identical_deviation = dat.identical_deviation ;
		this.Nbr_Items = dat.Nbr_Items ;
		this.Size_Bin = dat.Size_Bin ;
		this.Deviations = new ArrayList<>(dat.Deviations);
		this.Items_Sizes = new ArrayList<>(dat.Items_Sizes);
		this.Size_Deviation = dat.Size_Deviation_map();
	}

	// pour avoir des paires (size-deviation)
	public Map<Integer,Integer> Size_Deviation_map(){
		Map<Integer ,Integer> mp = new LinkedHashMap<>(); 
		for(int i=0; i<this.Nbr_Items; i++) {
			mp.put(this.Items_Sizes.get(i), this.Deviations.get(i));
		}
		return mp ;
	}


}

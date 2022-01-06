package modeles;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/*
 * La classe Solution contient le nombre de Bin necessaire pour l'instance
 * et toutes les configurations (Pattern) trouvées avec le nombre de copies de
 * chaque Item_i dans chque Configuration (a_i)
 */
public class Solution
{
	private Map<Configuration,Integer> Configurations;
	private int NbreDeBin;

	/*
	 * Le constructeur avec 
	 * L'instance et une Map de chaque configuration et le nombre de fois qu'on l'utilise 
	 */
	public Solution(Map<Configuration,Integer> solution)
	{

		this.Configurations = new LinkedHashMap<>(solution);
		this.NbreDeBin = Configurations.values()
				.stream()
				.mapToInt(i -> i)
				.sum();
	}

	// Retourner les Configurations utilisées dans cette Solution
	public Set<Configuration> getConfigurations()
	{
		return Collections.unmodifiableSet(Configurations.keySet());
	}

	// Retourner le Nombre de fois qu'on utilise la Configuration 'p' ( 0 sinon )
	//	public int getCopies(Configuration p)
	//	{
	//		return Configurations.getOrDefault(p, 0);
	//	}

	/*
	 * Le nombre de Bin pour couvrir la demande
	 */
	public int getNbreDeBin()
	{
		return NbreDeBin;
	}

	@Override
	public String toString()
	{
		return Configurations.toString();
	}
}



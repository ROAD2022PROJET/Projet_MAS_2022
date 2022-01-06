package modeles;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/*
 * La classe Solution contient le nombre de Bin necessaire pour l'instance
 * et toutes les configurations (Pattern) trouvées
 */
public class Solution
{
	private Map<Configuration,Integer> Configurations;
	private int NbreDeBin;

	
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



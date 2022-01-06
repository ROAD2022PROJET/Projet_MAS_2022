package modeles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Configuration {
	
	/*
	   dans cette Map les clés representent les indices des articles 
	   et les valeurs sont des booléens pour dire si on prens cet article dans ce Bin ou pas.  
	 */
	private Map<Integer,Integer> configuration;

	
	
	// Constructeur avec une Map 'value(index_size) = {1,0}' 
	public Configuration(Map<Integer,Integer> configuration /*,Données data*/)
	{
		this.configuration = new LinkedHashMap<>(configuration);
	}


	// si la configuration contient l'Item de taille ( size )
	public boolean containsSize(int index_size)
	{
		if(configuration.get(index_size) == 1  ) {
			return true;
		}
		return false  ;
	}
	

	/*
	// la liste des sizes présents dans la configuration 
	public Set<Integer> getSizes()
	{
		return Collections.unmodifiableSet(configuration.keySet());
	}
	 */

	
	// transformer la Map en List  
	public List<Integer> asList()
	{
		return configuration.entrySet()
				.stream()
				.flatMap(e -> Stream.generate(e::getKey).limit(e.getValue()))
				.collect(Collectors.toList());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((configuration == null) ? 0 : configuration.hashCode());
		result = prime * result + ( this.asList().get(0)+ 1);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Configuration other = (Configuration) obj;
		if (configuration == null) {
			if (other.configuration != null)
				return false;
		} else if (!configuration.equals(other.configuration))
			return false;
		if (! this.asList().equals(other.asList()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return asList().toString();
	}

}


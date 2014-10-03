package mai.cn.film.domain;

import java.util.SortedMap;
import java.util.TreeMap;

public class MovieUsers extends RateSet implements GraphNode {
	
	int idGraph;
	Type type;
	int idmovie;
	
	SortedMap<Integer,UserRate> userRates;
	
	public MovieUsers(int idmovie){
		super();
		this.idmovie=idmovie;
		this.idGraph=-1;
		this.type=Type.MOVIE;
		userRates=new TreeMap<Integer,UserRate>();		
	}
	
	public void addMovieRate(UserRate ur){
		userRates.put(ur.getIduser(), ur);
		updateAvgRate(ur.getRate(),userRates.size());	
	}
	
	public int numberOfRates(){
		return userRates.size();
	}

	public SortedMap<Integer, UserRate> getUserRates() {
		return userRates;
	}

	public void setUserRates(SortedMap<Integer, UserRate> userRates) {
		this.userRates = userRates;
	}

	@Override
	public SortedMap<Integer, ? extends Rate> getRates() {
		return userRates;
	}

	@Override
	public int getId() {
		return idmovie;
	}

	@Override
	public void setIdxNode(int idx) {
		idGraph=idx;
	}

	@Override
	public int getIdxNode() {
		return idGraph;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public String getLabel() {
		return "m"+getId();
	}
	
}

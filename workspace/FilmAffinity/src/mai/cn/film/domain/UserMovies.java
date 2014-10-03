package mai.cn.film.domain;


import java.util.SortedMap;
import java.util.TreeMap;

public class UserMovies extends RateSet implements GraphNode {
	
	int idGraph;
	Type type;
	int iduser;
	SortedMap<Integer,MovieRate> movieRates;
	
	public UserMovies(int iduser){
		this.iduser=iduser;
		this.idGraph=-1;
		this.type=Type.USER;
		movieRates=new TreeMap<Integer,MovieRate>();		
	}
	
	public void addMovieRate(MovieRate ur){
		movieRates.put(ur.getIdmovie(), ur);
		updateAvgRate(ur.getRate(),movieRates.size());
	}
	
	public int numberOfRates(){
		return movieRates.size();
	}

	public SortedMap<Integer, MovieRate> getMovieRates() {
		return movieRates;
	}

	public void setMovieRates(SortedMap<Integer, MovieRate> movieRates) {
		this.movieRates = movieRates;
	}

	@Override
	public SortedMap<Integer, ? extends Rate> getRates() {
		return movieRates;
	}

	@Override
	public int getId() {
		return iduser;
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
		return "u"+getId();
	}

	public int getRateForMovie(int id2movie) {
		MovieRate mr=movieRates.get(id2movie);
		int rate;
		if (mr!=null)
			rate=mr.getRate();
		else rate=-1;
			
		return rate;
	}

}

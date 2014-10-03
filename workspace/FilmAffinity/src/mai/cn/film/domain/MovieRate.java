package mai.cn.film.domain;

public class MovieRate extends Rate implements GraphNode{

	int idmovie;
	int rate;
	
	public MovieRate(int idmovie, int rate){
		super();
		this.idmovie=idmovie;
		this.rate=rate;
	}

	public int getIdmovie() {
		return idmovie;
	}

	public void setIdmovie(int idmovie) {
		this.idmovie = idmovie;
	}

	public void setRate(int rate) {
		this.rate=rate;
	}

	@Override
	public int getId() {		
		return getIdmovie();
	}

	@Override
	public int getRate() {
		return rate;
	}

	@Override
	public Type getType() {
		return Type.MOVIE;
	}

	@Override
	public String getLabel() {
		return "m"+getId();
	}

}

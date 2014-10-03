package mai.cn.film.domain;

public class MeMovieRate extends MovieRate {

	float avgRate;
	float avgMovieRate;
	float avgUserRate;
	
	public MeMovieRate(int idmovie, int rate) {
		super(idmovie, rate);	
	}

	public float getAvgRate() {
		return avgRate;
	}

	public void setAvgRate(float avgRate) {
		this.avgRate = avgRate;
	}

	public float getAvgMovieRate() {
		return avgMovieRate;
	}

	public void setAvgMovieRate(float avgMovieRate) {
		this.avgMovieRate = avgMovieRate;
	}

	public float getAvgUserRate() {
		return avgUserRate;
	}

	public void setAvgUserRate(float avgUserRate) {
		this.avgUserRate = avgUserRate;
	}

}

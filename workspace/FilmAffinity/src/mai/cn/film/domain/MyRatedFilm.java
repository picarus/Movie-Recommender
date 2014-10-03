package mai.cn.film.domain;

public class MyRatedFilm extends Film { // used to construct the database (from parsed files)
	
	private int myrate;
	
	public MyRatedFilm(int filmId, int myrate) {
		this(filmId, myrate, -1.0f, 0,"");
	}
	
	public MyRatedFilm(int filmId, int myrate, float avgrate, int rateNumbers, String filmTitle) {
		super(filmId,avgrate,rateNumbers,filmTitle);
		this.filmId = filmId;
		this.myrate = myrate;
		this.avgrate = avgrate;
		this.rateNumbers = rateNumbers;
	}

	public int getMyrate() {
		return myrate;
	}
	
	public void setMyrate(int myrate) {
		this.myrate = myrate;
	}

}

package mai.cn.film.domain;

public class UserRecommendedFilm {

	
	private int filmId;
	private int userRate;
	private int user;
	private String filmTitle;

	
	public UserRecommendedFilm(int filmId, int userRate, int user) {
		this(filmId,userRate, user, "");
	}
	
	public UserRecommendedFilm(int filmId, int userRate, int user,
			String filmTitle) {
		super();
		this.filmId = filmId;
		this.userRate = userRate;
		this.user = user;
		this.filmTitle = filmTitle;
	}

	public int getFilmId() {
		return filmId;
	}

	public void setFilmId(int filmId) {
		this.filmId = filmId;
	}

	public int getUserRate() {
		return userRate;
	}

	public void setUserRate(int userRate) {
		this.userRate = userRate;
	}

	public int getUser() {
		return user;
	}

	public void setUser(int user) {
		this.user = user;
	}

	public String getFilmTitle() {
		return filmTitle;
	}

	public void setFilmTitle(String filmTitle) {
		this.filmTitle = filmTitle;
	}
	
	
	

}

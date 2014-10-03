package mai.cn.film.domain;

import java.util.Iterator;

import mai.cn.film.util.SortedArrayList;

public class Film {
	
	int filmId;
	String filmTitle;
	float avgrate;
	int rateNumbers;
	SortedArrayList<Affinity> affinities;
	
	public Film(int filmId, float avgrate, int rateNumbers) {
		this(filmId,avgrate,rateNumbers,"");
	}
	
	public Film(int filmId, float avgrate, int rateNumbers, String filmTitle) {
		super();
		this.filmId = filmId;
		this.filmTitle = filmTitle;
		this.avgrate = avgrate;
		this.rateNumbers = rateNumbers;
		affinities=new SortedArrayList<Affinity>();
	}
	
	public Iterator<Affinity> getAffinities(){
		return affinities.iterator();
	}
	
	public void addAffinity(Affinity aff){
		affinities.insertSorted(aff);
	}
	
	public int getFilmId() {
		return filmId;
	}
	
	public void setFilmId(int filmId) {
		this.filmId = filmId;
	}
	
	public float getAvgRate() {
		return avgrate;
	}
	
	public void setAvgrate(float avgrate) {
		this.avgrate = avgrate;
	}
	
	public int getRateNumbers() {
		return rateNumbers;
	}
	
	public void setRateNumbers(int rateNumbers) {
		this.rateNumbers = rateNumbers;
	}

	public String getFilmTitle() {
		return filmTitle;
	}

	public void setFilmTitle(String filmTitle) {
		this.filmTitle = filmTitle;
	}

}
